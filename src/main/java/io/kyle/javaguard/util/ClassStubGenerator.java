package io.kyle.javaguard.util;

import io.kyle.javaguard.bean.ClassRequiredInfos;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 基于 ASM Tree API 的 class 文件"空壳化"工具。
 * <p>
 * 给定一个原始 class 文件的字节数组，本工具会生成一个结构完整但代码为空的"存根"class，
 * 满足以下条件：
 * <ol>
 *   <li><b>方法体清空</b>：所有普通方法仅保留最短的合法返回指令（返回对应类型的默认值）。</li>
 *   <li><b>final 字段处理</b>：所有 final 字段（包括原本带有 {@code ConstantValue} 的）
 *       的常量值被清除；static final 字段在 {@code <clinit>} 中赋默认值，
 *       instance final 字段在构造器中赋默认值。</li>
 *   <li><b>构造器简化</b>：保留原始 super/this 调用目标不变，但将参数替换为默认值，
 *       并在其后补充 instance final 字段的默认值赋值；
 *       如果是 {@code this(...)} 委托调用，则不重复赋值实例字段。</li>
 *   <li><b>静态初始化块简化</b>：仅保留 static final 字段的默认值赋值。</li>
 *   <li><b>Attribute 精简</b>：只保留运行时必需的 attribute（签名、异常、注解、参数名等），
 *       移除调试信息（LineNumberTable 等）。</li>
 * </ol>
 *
 * <h3>典型用途</h3>
 * 在 class 加密场景下，先用本工具生成空壳 class 对外暴露，再将原始字节码加密存储，
 * 运行时由自定义 ClassLoader 解密并还原真实字节码。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * byte[] originalBytes = Files.readAllBytes(Paths.get("MyClass.class"));
 * byte[] stubBytes = ClassStubGenerator.generateStubClass(originalBytes);
 * }</pre>
 *
 * <h3>兼容性</h3>
 * 依赖 ASM 9.x，支持 JDK 8 ~ JDK 21 产生的 class 文件格式。
 */
public class ClassStubGenerator {

    private ClassStubGenerator() {
    }

    /**
     * 对原始 class 字节码执行空壳化转换，返回转换后的字节数组。
     * <p>
     * 处理流程：
     * <ol>
     *   <li>使用 {@link ClassReader} 解析原始字节码为 {@link ClassNode}。
     *       注意：不使用 {@link ClassReader#SKIP_DEBUG}，以便保留 LocalVariableTable
     *       中的参数名信息（框架注入等场景需要）。</li>
     *   <li>依次执行字段、构造器、静态初始化块、普通方法的空壳化处理。</li>
     *   <li>过滤多余的 attribute。</li>
     *   <li>使用 {@link ClassWriter#COMPUTE_FRAMES} 自动重新计算栈帧，
     *       生成最终字节码。</li>
     * </ol>
     *
     * @param originalClassBytes 原始 class 文件的字节内容，不得为 null
     * @return 空壳化后的 class 字节码
     */
    public static byte[] generateStubClass(byte[] originalClassBytes, Consumer<ClassNode> handle) {
        ClassReader classReader = new ClassReader(originalClassBytes);
        ClassNode classNode = new ClassNode();
        // 不使用 SKIP_DEBUG：需要保留 LocalVariableTable 中的参数名
        // 不使用 SKIP_FRAMES：后续 COMPUTE_FRAMES 会重新计算，但读取时仍需原始帧信息以正确解析
        classReader.accept(classNode, ClassReader.SKIP_FRAMES);

        ClassRequiredInfos requiredInfos = processFields(classNode);
        processConstructors(classNode, requiredInfos.fields);
        processStaticInitializer(classNode, requiredInfos.staticFields);
        removeSyntheticHelperMethods(classNode);
        processMethods(classNode);
        if (handle != null) {
            handle.accept(classNode);
        }

        // COMPUTE_FRAMES 隐含 COMPUTE_MAXS，自动计算 max_stack/max_locals 及 StackMapTable
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    // -------------------------------------------------------------------------
    // 字段处理
    // -------------------------------------------------------------------------

    /**
     * 清除所有字段的 {@code ConstantValue}（即 {@link FieldNode#value}）。
     * <p>
     * ASM Tree API 中，静态字段的 {@code ConstantValue} attribute 体现在 {@code field.value} 上；
     * 将其置为 null 可防止 ClassWriter 输出时自动还原该 attribute。
     * <p>
     * final 字段的实际默认值赋值由 {@link #processConstructors}（实例 final）
     * 和 {@link #processStaticInitializer}（static final）负责生成字节码指令；
     * 非 final 字段无需显式赋值，JVM 在分配对象/加载类时会自动零初始化。
     */
    private static ClassRequiredInfos processFields(ClassNode classNode) {
        if (classNode.fields == null) {
            return new ClassRequiredInfos(Collections.emptyList(), Collections.emptyList());
        }
        int cap = classNode.fields.size() >> 1;
        ArrayList<FieldNode> finalFields = new ArrayList<>(cap);
        ArrayList<FieldNode> staticFinalFields = new ArrayList<>(cap);
        for (FieldNode field : classNode.fields) {
            if (AsmUtils.isFinalField(field)) {
                (AsmUtils.isStaticField(field) ? staticFinalFields : finalFields).add(field);
            }
            field.value = null;
        }
        return new ClassRequiredInfos(finalFields, staticFinalFields);
    }

    // -------------------------------------------------------------------------
    // 构造器处理
    // -------------------------------------------------------------------------

    /**
     * 空壳化类中所有构造器（{@code <init>} 方法）。
     * <p>
     * 收集所有 instance final 字段，对每个构造器单独处理：
     * <ul>
     *   <li>如果构造器是 {@code this(...)} 委托调用（调用的是同一个类的 {@code <init>}），
     *       则不追加字段赋值，因为被委托的构造器最终会负责赋值，避免重复赋值。</li>
     *   <li>如果构造器是 {@code super(...)} 调用，则追加 instance final 字段的默认值赋值。</li>
     * </ul>
     */
    private static void processConstructors(ClassNode classNode, List<FieldNode> requireFields) {
        boolean processedConstructor = false;
        if (classNode.methods != null) {
            for (MethodNode method : classNode.methods) {
                if ("<init>".equals(method.name)) {
                    processConstructor(classNode, method, requireFields);
                    processedConstructor = true;
                }
            }
        }
        if (processedConstructor) {
            return;
        }

        if (!requireFields.isEmpty()) {
            if (AsmUtils.isInterface(classNode)) {
                // 正常的 class 文件不应出现此情况；出现时发出警告，并跳过
                System.err.println("WARN: No super/this constructor call found in "
                        + classNode.name + ": require field: " + StringUtils.join(requireFields.stream().map(f -> f.name).iterator(), ','));
            } else {
                processConstructor(classNode, null, requireFields);
            }
        }
    }

    /**
     * 空壳化单个构造器。
     *
     * @param classNode           当前类节点
     * @param constructor         待处理的构造器方法节点
     * @param instanceFinalFields 当前类的所有 instance final 字段
     */
    private static void processConstructor(ClassNode classNode, MethodNode constructor,
                                           List<FieldNode> instanceFinalFields) {
        MethodInsnNode superCall = null;
        if (constructor != null) {
            superCall = AsmUtils.findSuperConstructorCall(constructor);

            if (superCall == null) {
                // 正常的 class 文件不应出现此情况；出现时发出警告，并尝试调用父类无参构造器兜底
                System.err.println("WARN: No super/this constructor call found in "
                        + classNode.name + " " + constructor.name + constructor.desc);
            }
        }

        InsnList newInstructions = new InsnList();

        if (superCall == null) {
            // 兜底：尝试调用父类无参构造器，保持字节码合法性
            newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            newInstructions.add(new MethodInsnNode(
                    Opcodes.INVOKESPECIAL, classNode.superName, "<init>", "()V", false));
        } else {
            // 保留原始的 super/this 调用目标，但所有参数替换为默认值
            newInstructions.add(AsmUtils.createSuperCallInstructions(superCall));
        }

        // 仅当是 super(...) 调用时才赋值 instance final 字段；
        // this(...) 委托调用最终会走到某个 super(...) 构造器，由那条路径完成赋值
        boolean isDelegatingThis = superCall != null && classNode.name.equals(superCall.owner);
        if (!isDelegatingThis && !instanceFinalFields.isEmpty()) {
            newInstructions.add(AsmUtils.createFieldInitInstructions(classNode, instanceFinalFields, false));
        }

        newInstructions.add(new InsnNode(Opcodes.RETURN));

        if (constructor == null) {
            constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            if (classNode.methods == null) {
                classNode.methods = new ArrayList<>();
            }
            classNode.methods.add(constructor);
        }
        replaceMethodBody(constructor, newInstructions, classNode.name);
    }

    // -------------------------------------------------------------------------
    // 静态初始化块处理
    // -------------------------------------------------------------------------

    /**
     * 空壳化静态初始化块（{@code <clinit>}）。
     * <p>
     * <ul>
     *   <li>如果类中存在 static final 字段，则创建或替换 {@code <clinit>}，
     *       内容仅为对这些字段赋默认值。</li>
     *   <li>如果类中没有 static final 字段，则移除已有的 {@code <clinit>}（如果有），
     *       避免保留无意义的空方法体。</li>
     * </ul>
     */
    private static void processStaticInitializer(ClassNode classNode, List<FieldNode> requireFields) {
        // 直接移除 <clinit>
        removeStaticInitializer(classNode);
        if (requireFields.isEmpty()) {
            return;
        }

        MethodNode clinit = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);

        InsnList newInstructions = new InsnList();
        newInstructions.add(AsmUtils.createFieldInitInstructions(classNode, requireFields, true));
        newInstructions.add(new InsnNode(Opcodes.RETURN));

        clinit.instructions.add(newInstructions);
        clinit.tryCatchBlocks = new ArrayList<>();

        if (classNode.methods == null) {
            classNode.methods = new ArrayList<>(1);
        }
        classNode.methods.add(clinit);
    }

    /**
     * 移除类中的 {@code <clinit>} 方法（如果存在）。
     */
    private static void removeStaticInitializer(ClassNode classNode) {
        if (classNode.methods != null) {
            classNode.methods.removeIf(method -> "<clinit>".equals(method.name));
        }
    }

    // -------------------------------------------------------------------------
    // 普通方法处理
    // -------------------------------------------------------------------------

    /**
     * 移除由编译器自动生成、在空壳类中已无意义的 synthetic 辅助方法。
     * <p>
     * 所有方法体均已通过 {@code instructions.clear()} 清空，{@code invokedynamic} 指令也随之删除。
     * 因此 {@code BootstrapMethods} 中不再有任何有效入口，lambda 脱糖方法的"被引用"前提已不成立。
     * <p>
     * 可安全删除的 synthetic 方法：
     * <ul>
     *   <li>{@code lambda$xxx$N} - lambda 脱糖方法，其调用方（invokedynamic）已随方法体清空而消失</li>
     *   <li>{@code access$xxx}   - 内部类访问外围类私有成员的桥接方法，调用方代码已清空</li>
     * </ul>
     * 保留的 synthetic 方法：
     * <ul>
     *   <li>桥接方法（{@code ACC_BRIDGE}）- 泛型擦除产生，反射/序列化框架可能通过方法签名查找</li>
     *   <li>其余 synthetic（如 Enum 的 {@code values()}/{@code valueOf()}）- 公开 API，不删除</li>
     * </ul>
     */
    private static void removeSyntheticHelperMethods(ClassNode classNode) {
        if (classNode.methods == null) {
            return;
        }
        classNode.methods.removeIf(method -> {
            if ((method.access & Opcodes.ACC_SYNTHETIC) == 0) {
                return false;
            }
            // bridge 方法（泛型擦除产生）必须保留，反射场景可能依赖
            if ((method.access & Opcodes.ACC_BRIDGE) != 0) {
                return false;
            }
            // lambda 脱糖方法：invokedynamic 调用方已随方法体清空，可以删除
            if (method.name.startsWith("lambda$")) {
                return true;
            }
            // access$xxx：内部类访问桥接，调用方代码已清空，可以删除
            if (method.name.startsWith("access$")) {
                return true;
            }
            // 其余 synthetic 方法（如 Enum 的 values()/valueOf() 等）保留
            return false;
        });
    }

    /**
     * 空壳化所有普通方法（跳过 {@code <init>} 和 {@code <clinit>}）。
     * <p>
     * abstract 方法和 native 方法在 class 文件中没有 Code attribute，不得添加方法体，直接跳过。
     * 其余方法的方法体被替换为：压入对应返回类型的默认值（void 则不压），然后执行 xRETURN。
     */
    private static void processMethods(ClassNode classNode) {
        if (classNode.methods == null) {
            return;
        }
        for (MethodNode method : classNode.methods) {
            if ("<init>".equals(method.name) || "<clinit>".equals(method.name)) {
                continue;
            }
            // abstract 和 native 方法没有方法体，不能添加 Code attribute
            if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
                continue;
            }
            processRegularMethod(classNode.name, method);
        }
    }

    /**
     * 空壳化单个普通方法。
     * <p>
     * 新方法体结构：
     * <pre>
     *   [xCONST_0 / ACONST_NULL]  // 非 void 返回类型时压入默认值
     *   xRETURN                    // 对应类型的返回指令
     * </pre>
     */
    private static void processRegularMethod(String className, MethodNode method) {
        Type returnType = Type.getReturnType(method.desc);
        InsnList newInstructions = new InsnList();
        // 非 void 方法需要先压入默认返回值
        int defaultValueOpcode = AsmUtils.getDefaultValueOpcode(returnType);
        if (defaultValueOpcode != Opcodes.NOP) {
            newInstructions.add(new InsnNode(defaultValueOpcode));
        }
        newInstructions.add(new InsnNode(AsmUtils.getReturnOpcode(returnType)));
        // 静态方法传 null 表示不生成 this 条目
        replaceMethodBody(method, newInstructions, (method.access & Opcodes.ACC_STATIC) != 0 ? null : className);
    }

    // -------------------------------------------------------------------------
    // 公共辅助方法
    // -------------------------------------------------------------------------

    /**
     * 用新指令序列替换方法的全部字节码，并同时清理、重建关联的运行时数据。
     * <p>
     * 处理内容：
     * <ul>
     *   <li>{@code tryCatchBlocks}：清空，原有 try-catch 对新方法体无意义</li>
     *   <li>{@code localVariables}：清空后重建，仅保留参数条目（兼容依赖
     *       {@code LocalVariableTable} 读取参数名的框架，如旧版 Spring）；
     *       参数名优先取自 {@code MethodNode.parameters}，否则退化为 {@code arg0/arg1/...}</li>
     * </ul>
     * 注：{@code MethodNode.parameters}（对应 {@code MethodParameters} attribute）不受影响，
     * Spring Boot 6+ 等新版框架直接使用该字段，无需 {@code LocalVariableTable}。
     *
     * @param method          目标方法节点
     * @param newInstructions 替换用的新指令序列
     * @param className       所属类的内部名（如 {@code com/example/Foo}），仅用于生成 this 条目；
     *                        静态方法或无需 this 条目时传 null
     */
    private static void replaceMethodBody(MethodNode method, InsnList newInstructions, String className) {
        // 用首尾两个 LabelNode 包裹指令，作为 LocalVariableTable 范围边界
        LabelNode startLabel = new LabelNode();
        LabelNode endLabel = new LabelNode();

        method.instructions.clear();
        method.instructions.add(startLabel);
        method.instructions.add(newInstructions);
        method.instructions.add(endLabel);

        method.tryCatchBlocks = new ArrayList<>();

        // 局部变量类型注解（@TypeAnnotation 作用于局部变量）内含 LabelNode 范围引用，
        // 指令清空后这些 Label 已失效，必须清空，否则 ClassWriter 输出非法字节码
        method.visibleLocalVariableAnnotations = null;
        method.invisibleLocalVariableAnnotations = null;

        // 重建只含参数的 LocalVariableTable
        if (method.localVariables == null) {
            method.localVariables = new ArrayList<>();
        } else {
            method.localVariables.clear();
        }

        boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
        Type[] argTypes = Type.getArgumentTypes(method.desc);
        int slot = 0;

        if (!isStatic && className != null) {
            // slot 0: this，descriptor 使用类自身的对象类型
            method.localVariables.add(new LocalVariableNode(
                    "this", "L" + className + ";", null, startLabel, endLabel, slot));
            slot++;
        }

        for (int i = 0; i < argTypes.length; i++) {
            String paramName;
            if (method.parameters != null && i < method.parameters.size()) {
                paramName = method.parameters.get(i).name;
            } else {
                paramName = "arg" + i;
            }
            method.localVariables.add(new LocalVariableNode(
                    paramName, argTypes[i].getDescriptor(), null, startLabel, endLabel, slot));
            // long 和 double 占两个 slot
            slot += argTypes[i].getSize();
        }
    }
}
