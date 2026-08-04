package io.kyle.javaguard.test;

import io.kyle.javaguard.util.ClassStubGenerator;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;

public class ClassStubGeneratorTest {
    private static final String CLASS_NAME = "io.kyle.javaguard.test.GeneratedStubSubject";

    @Test
    public void generatedStubLoadsAndReturnsDefaultValues() throws Exception {
        byte[] original = subjectClass();
        byte[] stub = ClassStubGenerator.generateStubClass(original, null);

        Assert.assertNotNull(stub);
        Assert.assertTrue(stub.length > 0);
        Assert.assertFalse("stub must differ from the implementation class",
                java.util.Arrays.equals(original, stub));

        Class<?> type = new IsolatedClassLoader().define(CLASS_NAME, stub);
        Object instance = type.getConstructor().newInstance();
        Assert.assertEquals(0, type.getMethod("number").invoke(instance));
        Assert.assertEquals(false, type.getMethod("flag").invoke(instance));
        Assert.assertNull(type.getMethod("text").invoke(instance));
    }

    @Test
    public void generationIsDeterministicAndPreservesNativeMethods() throws Exception {
        byte[] original = subjectClass();
        byte[] first = ClassStubGenerator.generateStubClass(original, null);
        byte[] second = ClassStubGenerator.generateStubClass(original, null);

        Assert.assertArrayEquals(first, second);
        Class<?> type = new IsolatedClassLoader().define(CLASS_NAME, first);
        Assert.assertTrue(Modifier.isNative(type.getDeclaredMethod("nativeCall").getModifiers()));
    }

    @Test
    public void unnamedMethodParameterRemainsUnnamed() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "test/UnnamedParameter", null,
                "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                "bridge", "(Ljava/lang/Object;)V", null, null);
        method.visitParameter(null, Opcodes.ACC_SYNTHETIC);
        Label start = new Label();
        Label end = new Label();
        method.visitCode();
        method.visitLabel(start);
        method.visitInsn(Opcodes.RETURN);
        method.visitLabel(end);
        method.visitLocalVariable("sourceName", "Ljava/lang/Object;", null, start, end, 1);
        method.visitMaxs(0, 2);
        method.visitEnd();
        writer.visitEnd();

        byte[] first = ClassStubGenerator.generateStubClass(writer.toByteArray(), null);
        byte[] second = ClassStubGenerator.generateStubClass(writer.toByteArray(), null);

        Assert.assertArrayEquals(first, second);
        final boolean[] unnamedParameter = {false};
        final boolean[] originalLocalName = {false};
        final boolean[] inventedLocalName = {false};
        new ClassReader(first).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"bridge".equals(name)) {
                    return visitor;
                }
                return new MethodVisitor(Opcodes.ASM9, visitor) {
                    @Override
                    public void visitParameter(String name, int access) {
                        unnamedParameter[0] = name == null;
                        super.visitParameter(name, access);
                    }

                    @Override
                    public void visitLocalVariable(String name, String descriptor, String signature,
                                                   org.objectweb.asm.Label start, org.objectweb.asm.Label end,
                                                   int index) {
                        originalLocalName[0] |= "sourceName".equals(name) && index == 1;
                        inventedLocalName[0] |= "arg0".equals(name);
                        super.visitLocalVariable(name, descriptor, signature, start, end, index);
                    }
                };
            }
        }, 0);
        Assert.assertTrue("MethodParameters name must remain absent", unnamedParameter[0]);
        Assert.assertTrue("original LocalVariableTable parameter name must be preserved", originalLocalName[0]);
        Assert.assertFalse("stub must not invent an arg0 LocalVariableTable name", inventedLocalName[0]);
    }

    @Test
    public void laterSlotReuseDoesNotReplaceParameterMetadata() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "test/SlotReuse", null,
                "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "reuse", "(Ljava/lang/String;)V", null, null);
        method.visitParameter("declaredName", 0);
        Label start = new Label();
        Label reusedStart = new Label();
        Label end = new Label();
        method.visitCode();
        method.visitLabel(start);
        method.visitInsn(Opcodes.NOP);
        method.visitLabel(reusedStart);
        method.visitInsn(Opcodes.RETURN);
        method.visitLabel(end);
        method.visitLocalVariable("laterLocal", "Ljava/lang/String;", null, reusedStart, end, 0);
        method.visitMaxs(0, 1);
        method.visitEnd();
        writer.visitEnd();

        byte[] stub = ClassStubGenerator.generateStubClass(writer.toByteArray(), null);
        final boolean[] laterLocal = {false};
        new ClassReader(stub).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"reuse".equals(name)) {
                    return visitor;
                }
                return new MethodVisitor(Opcodes.ASM9, visitor) {
                    @Override
                    public void visitLocalVariable(String name, String descriptor, String signature,
                                                   Label start, Label end, int index) {
                        laterLocal[0] |= "laterLocal".equals(name) && index == 0;
                        super.visitLocalVariable(name, descriptor, signature, start, end, index);
                    }
                };
            }
        }, 0);
        Assert.assertFalse("a later same-slot local must not be promoted to parameter metadata", laterLocal[0]);
    }

    static byte[] subjectClass() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, CLASS_NAME.replace('.', '/'), null,
                "java/lang/Object", null);

        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(1, 1);
        constructor.visitEnd();

        addConstantMethod(writer, "number", "()I", Opcodes.BIPUSH, 42, Opcodes.IRETURN);
        addConstantMethod(writer, "flag", "()Z", Opcodes.ICONST_1, 0, Opcodes.IRETURN);

        MethodVisitor text = writer.visitMethod(Opcodes.ACC_PUBLIC, "text", "()Ljava/lang/String;", null, null);
        text.visitCode();
        text.visitLdcInsn("sensitive method body");
        text.visitInsn(Opcodes.ARETURN);
        text.visitMaxs(1, 1);
        text.visitEnd();

        writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_NATIVE, "nativeCall", "()V", null, null).visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void addConstantMethod(ClassWriter writer, String name, String descriptor,
                                          int constantOpcode, int operand, int returnOpcode) {
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, name, descriptor, null, null);
        method.visitCode();
        if (constantOpcode == Opcodes.BIPUSH) {
            method.visitIntInsn(constantOpcode, operand);
        } else {
            method.visitInsn(constantOpcode);
        }
        method.visitInsn(returnOpcode);
        method.visitMaxs(1, 1);
        method.visitEnd();
    }

    private static final class IsolatedClassLoader extends ClassLoader {
        private IsolatedClassLoader() {
            super(null);
        }

        private Class<?> define(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
