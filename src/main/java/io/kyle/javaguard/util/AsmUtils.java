package io.kyle.javaguard.util;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.List;

/**
 * ASM 字节码操作工具类，为 {@link ClassStubGenerator} 提供底层支持。
 * <p>
 * 主要职责：
 * <ul>
 *   <li>类型相关的操作码映射（默认值指令、返回指令）</li>
 *   <li>构造器 super/this 调用的查找</li>
 *   <li>final 字段的收集与初始化指令生成</li>
 *   <li>class 文件 attribute 的保留策略判断与过滤</li>
 * </ul>
 */
public class AsmUtils {

    private AsmUtils() {
    }

    /**
     * 获取指定类型的"默认值"加载操作码。
     * <ul>
     *   <li>void      -> {@link Opcodes#NOP}（调用方应跳过，不生成指令）</li>
     *   <li>boolean/char/byte/short/int -> {@link Opcodes#ICONST_0}</li>
     *   <li>long      -> {@link Opcodes#LCONST_0}</li>
     *   <li>float     -> {@link Opcodes#FCONST_0}</li>
     *   <li>double    -> {@link Opcodes#DCONST_0}</li>
     *   <li>array/object -> {@link Opcodes#ACONST_NULL}</li>
     * </ul>
     */
    public static int getDefaultValueOpcode(Type type) {
        switch (type.getSort()) {
            case Type.VOID:
                return Opcodes.NOP;
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                return Opcodes.ICONST_0;
            case Type.LONG:
                return Opcodes.LCONST_0;
            case Type.FLOAT:
                return Opcodes.FCONST_0;
            case Type.DOUBLE:
                return Opcodes.DCONST_0;
            default:
                // ARRAY / OBJECT 及未知类型统一返回 null
                return Opcodes.ACONST_NULL;
        }
    }

    /**
     * 获取指定返回类型对应的 xRETURN 操作码。
     * <ul>
     *   <li>void      -> {@link Opcodes#RETURN}</li>
     *   <li>boolean/char/byte/short/int -> {@link Opcodes#IRETURN}</li>
     *   <li>long      -> {@link Opcodes#LRETURN}</li>
     *   <li>float     -> {@link Opcodes#FRETURN}</li>
     *   <li>double    -> {@link Opcodes#DRETURN}</li>
     *   <li>array/object -> {@link Opcodes#ARETURN}</li>
     * </ul>
     */
    public static int getReturnOpcode(Type type) {
        switch (type.getSort()) {
            case Type.VOID:
                return Opcodes.RETURN;
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                return Opcodes.IRETURN;
            case Type.LONG:
                return Opcodes.LRETURN;
            case Type.FLOAT:
                return Opcodes.FRETURN;
            case Type.DOUBLE:
                return Opcodes.DRETURN;
            default:
                // ARRAY / OBJECT 及未知类型
                return Opcodes.ARETURN;
        }
    }

    /**
     * 为给定类型创建"压入默认值"的指令序列。
     * <p>
     * void 类型不产生任何指令（返回空列表）；
     * 其余类型产生单条 xCONST_0 / ACONST_NULL 指令。
     */
    public static InsnList createDefaultValueInsn(Type type) {
        InsnList insnList = new InsnList();
        int opcode = getDefaultValueOpcode(type);
        if (opcode != Opcodes.NOP) {
            insnList.add(new InsnNode(opcode));
        }
        return insnList;
    }

    /**
     * 在构造器指令流中查找第一条真正属于"调用父类或委托构造器"的 INVOKESPECIAL &lt;init&gt; 指令。
     * <p>
     * 通过跟踪 NEW 指令的嵌套深度，跳过方法体内通过 {@code new Foo(...)} 创建对象
     * 所产生的 INVOKESPECIAL，只返回深度为 0（即当前对象自身的初始化委托）的那条调用。
     *
     * @param constructor 构造器方法节点
     * @return super/this 构造器调用的指令节点，找不到则返回 null
     */
    public static MethodInsnNode findSuperConstructorCall(MethodNode constructor) {
        if (constructor == null || constructor.instructions == null) {
            return null;
        }
        // newDepth 用于区分"对象内部 new Foo()" 与"当前构造器的 super/this 调用"
        // 遇到 NEW 则深度+1，遇到匹配的 INVOKESPECIAL <init> 则深度-1
        // 深度为 0 时命中的 INVOKESPECIAL <init> 即为目标
        int newDepth = 0;
        for (AbstractInsnNode insn : constructor.instructions) {
            if (insn.getOpcode() == Opcodes.NEW) {
                newDepth++;
            } else if (insn.getOpcode() == Opcodes.INVOKESPECIAL) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                if ("<init>".equals(methodInsn.name)) {
                    if (newDepth == 0) {
                        return methodInsn;
                    }
                    newDepth--;
                }
            }
        }
        return null;
    }

    /**
     * 判断字段是否带有 {@code final} 修饰符。
     */
    public static boolean isFinalField(FieldNode field) {
        return (field.access & Opcodes.ACC_FINAL) != 0;
    }

    /**
     * 判断字段是否带有 {@code static} 修饰符。
     */
    public static boolean isStaticField(FieldNode field) {
        return (field.access & Opcodes.ACC_STATIC) != 0;
    }

    /**
     * 判断class是否是接口。
     */
    public static boolean isInterface(ClassNode classNode) {
        return (classNode.access & Opcodes.ACC_INTERFACE) != 0;
    }

    /**
     * 为给定字段列表生成"使用默认值赋值"的字节码指令序列。
     * <p>
     * 对于实例字段（!isStatic），每条赋值前会先压入 {@code ALOAD 0}（this 引用）；
     * 对于静态字段，直接压入默认值后执行 PUTSTATIC。
     *
     * @param classNode 字段所属类的节点（用于生成正确的 owner 引用）
     * @param fields    需要赋值的字段列表
     * @param isStatic  true 表示生成 PUTSTATIC，false 表示生成 PUTFIELD
     * @return 包含所有赋值指令的 InsnList
     */
    public static InsnList createFieldInitInstructions(ClassNode classNode, List<FieldNode> fields, boolean isStatic) {
        InsnList insnList = new InsnList();
        for (FieldNode field : fields) {
            if (!isStatic) {
                // 实例字段赋值需要先加载 this 引用
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
            }
            Type fieldType = Type.getType(field.desc);
            // 压入对应类型的默认值
            insnList.add(createDefaultValueInsn(fieldType));
            // 执行赋值
            int putOpcode = isStatic ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD;
            insnList.add(new FieldInsnNode(putOpcode, classNode.name, field.name, field.desc));
        }
        return insnList;
    }

    /**
     * 根据原始的 super/this 构造器调用节点，生成替代性的调用指令序列。
     * <p>
     * 保留原始调用的 owner、name、desc（即保持调用的是同一个方法），
     * 但将所有实参替换为对应类型的默认值（0 / false / null）。
     * 生成的指令为：
     * <pre>
     *   ALOAD 0
     *   [默认值参数1]
     *   [默认值参数2]
     *   ...
     *   INVOKESPECIAL owner.&lt;init&gt;(desc)
     * </pre>
     *
     * @param superCall 原始构造器调用指令（由 {@link #findSuperConstructorCall} 找到）
     * @return 替代调用的指令序列
     */
    public static InsnList createSuperCallInstructions(MethodInsnNode superCall) {
        InsnList insnList = new InsnList();
        // 压入 this 引用
        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
        // 逐个参数压入默认值
        for (Type argType : Type.getArgumentTypes(superCall.desc)) {
            insnList.add(createDefaultValueInsn(argType));
        }
        // 重建 INVOKESPECIAL 指令，保持原始调用目标不变
        insnList.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                superCall.owner,
                superCall.name,
                superCall.desc,
                false
        ));
        return insnList;
    }
}
