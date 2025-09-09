package io.kyle.javaguard.transform;

import io.kyle.javaguard.bean.ClassTransformInfo;
import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.constant.ClassAttribute;
import io.kyle.javaguard.constant.DefaultReturn;
import io.kyle.javaguard.exception.TransformException;
import io.kyle.javaguard.exception.TransformRuntimeException;
import javassist.bytecode.*;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.AnnotationExt;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.ListIterator;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/9/30 14:17
 */
public class ClassTransformer extends AbstractTransformer {
    public static final int CLASS_ENCRYPT_FLAG = 1 << 15;

    public ClassTransformer(TransformInfo transformInfo) {
        super(transformInfo);
    }

    @Override
    public boolean isSupport(String name) {
        return name.endsWith(".class");
    }

    @Override
    public boolean encrypt(InputStream in, OutputStream out) throws TransformException {
        ClassFile classFile = null;
        try {
            classFile = new ClassFile(new DataInputStream(in));
        } catch (IOException e) {
            throw new TransformException("analysis class byte failed", e);
        }
        ConstPool constPool = classFile.getConstPool();
        ClassTransformInfo classTransformInfo = new ClassTransformInfo(constPool);
        for (FieldInfo field : classFile.getFields()) {
            classTransformInfo.addRetainConst(JavassistExt.fieldNameIndex(field));
            classTransformInfo.addRetainConst(JavassistExt.fieldDescriptorIndex(field));
            for (AttributeInfo attribute : field.getAttributes()) {
                handleAttribute(attribute, classTransformInfo);
            }
        }

        for (MethodInfo method : classFile.getMethods()) {
            handleMethod(method, classTransformInfo);
        }

        MethodInfo staticInitializer = classFile.getStaticInitializer();
        handleMethod(staticInitializer, classTransformInfo);

        for (AttributeInfo attribute : classFile.getAttributes()) {
            handleAttribute(attribute, classTransformInfo);
        }
        JavassistExt.retainClassInfoConst(classTransformInfo.getConstPool(), classTransformInfo.getRetainConst());

        byte[] bytes = ClassTransformUtils.toBytes(classTransformInfo);
        try {
            classFile.addAttribute(new SecretBoxAttribute(constPool, transformInfo.getKeyInfo().encrypt(bytes)));
        } catch (Exception e) {
            throw new TransformException("class encrypt failed", e);
        }

        try {
            classFile.write(new DataOutputStream(out));
        } catch (IOException e) {
            throw new TransformException("write class byte failed", e);
        }
        return true;
    }

    @Override
    public boolean decrypt(InputStream in, OutputStream out) throws TransformException {
        byte[] classByte;
        try {
            classByte = IOUtils.toByteArray(in);
            byte[] bytes = ClassDecryption.decryptClass(classByte, data -> {
                try {
                    return transformInfo.getKeyInfo().decrypt(data);
                } catch (Exception e) {
                    throw new TransformRuntimeException("decrypt failed", e);
                }
            });
            out.write(bytes);
        } catch (IOException e) {
            throw new TransformException("decrypt class data failed", e);
        }
        return true;
    }

    protected void handleMethod(MethodInfo method, ClassTransformInfo classTransformInfo) {
        if (method == null) {
            return;
        }
        classTransformInfo.addRetainConst(JavassistExt.methodNameIndex(method));
        classTransformInfo.addRetainConst(JavassistExt.methodDescriptorIndex(method));
        ListIterator<AttributeInfo> iterator = method.getAttributes().listIterator();
        while (iterator.hasNext()) {
            AttributeInfo attribute = iterator.next();
            AttributeInfo attributeInfo;
            if (attribute instanceof CodeAttribute) {
                attributeInfo = handleCodeAttribute((CodeAttribute) attribute, classTransformInfo, method.getDescriptor());
            } else {
                attributeInfo = handleAttribute(attribute, classTransformInfo);
            }
            if (attributeInfo != attribute) {
                iterator.set(attributeInfo);
            }
        }
//        CodeAttribute codeAttribute = method.getCodeAttribute();
//        if (codeAttribute != null) {
//            method.setCodeAttribute(handleCodeAttribute(codeAttribute, classTransformInfo, method.getDescriptor()));
//        }
    }

    protected CodeAttribute handleCodeAttribute(CodeAttribute codeAttribute, ClassTransformInfo classTransformInfo, String descriptor) {
        if (codeAttribute == null) {
            return null;
        }
        for (AttributeInfo codeAttributeAttribute : codeAttribute.getAttributes()) {
            handleAttribute(codeAttributeAttribute, classTransformInfo);
        }
        if (codeAttribute.getCodeLength() == 0 ||
                (codeAttribute.getCodeLength() < 2 && codeAttribute.getCode()[0] == (byte) Bytecode.RETURN)) {
            classTransformInfo.addCode(null);
            return codeAttribute;
        }
//                codeAttribute.getExceptionTable()
        DefaultReturn defaultReturn = DefaultReturn.byDescriptor(descriptor);
        Bytecode bytecode = new Bytecode(classTransformInfo.getConstPool());
        for (int opcode : defaultReturn.getOpcodes()) {
            bytecode.addOpcode(opcode);
        }

        CodeAttribute newCodeAttribute = new CodeAttribute(codeAttribute.getConstPool(),
                bytecode.getMaxStack(), bytecode.getMaxLocals(), bytecode.get(),
                codeAttribute.getExceptionTable());
        newCodeAttribute.getAttributes().addAll(codeAttribute.getAttributes());
        // 暂不需要index了，解密时直接按序解密
//        newCodeAttribute.getAttributes()
//                .add(new CodeIndexAttribute(codeAttribute.getConstPool(), classTransformInfo.codesSize()));
        classTransformInfo.addCode(codeAttribute);
        return newCodeAttribute;
    }

    protected AttributeInfo handleAttribute(AttributeInfo attributeInfo, ClassTransformInfo classTransformInfo) {
        ClassAttribute classAttribute = ClassAttribute.of(attributeInfo.getName());
        if (classAttribute == null) {
            return attributeInfo;
        }
        classTransformInfo.addRetainConst(JavassistExt.attributeNameIndex(attributeInfo));
        switch (classAttribute) {
            case AnnotationDefault:
                // ignore
                break;
            case RuntimeInvisibleAnnotations:
            case RuntimeVisibleAnnotations:
                AnnotationsAttribute attribute = (AnnotationsAttribute) attributeInfo;
                for (Annotation annotation : attribute.getAnnotations()) {
                    classTransformInfo.addRetainConst(AnnotationExt.annotationAttributeTypeIndex(annotation));
                    AnnotationExt.foreachAnnotationAttributeMembers(annotation,
                            (nameIndex, memberValue) -> classTransformInfo.addRetainConst(nameIndex));
                }
                break;
            case BootstrapMethods:
                BootstrapMethodsAttribute bootstrapMethodsAttribute = (BootstrapMethodsAttribute) attributeInfo;
                for (BootstrapMethodsAttribute.BootstrapMethod method : bootstrapMethodsAttribute.getMethods()) {
                    classTransformInfo.addRetainConst(method.methodRef);
                    if (method.arguments == null) {
                        continue;
                    }
                    for (int argument : method.arguments) {
                        classTransformInfo.addRetainConst(argument);
                    }
                }
                break;
            case Code:
                CodeAttribute codeAttribute = (CodeAttribute) attributeInfo;
                return handleCodeAttribute(codeAttribute, classTransformInfo, null);
            case EnclosingMethod:
                EnclosingMethodAttribute enclosingMethodAttribute = (EnclosingMethodAttribute) attributeInfo;
                classTransformInfo.addRetainConst(enclosingMethodAttribute.classIndex());
                classTransformInfo.addRetainConst(enclosingMethodAttribute.methodIndex());
                break;
            case Exceptions:
                ExceptionsAttribute exceptionsAttribute = (ExceptionsAttribute) attributeInfo;
                for (int exceptionIndex : exceptionsAttribute.getExceptionIndexes()) {
                    classTransformInfo.addRetainConst(exceptionIndex);
                }
                break;
            case InnerClasses:
                InnerClassesAttribute innerClassesAttribute = (InnerClassesAttribute) attributeInfo;
                int icLen = innerClassesAttribute.tableLength();
                for (int i = 0; i < icLen; i++) {
                    classTransformInfo.addRetainConst(innerClassesAttribute.outerClassIndex(i));
                    classTransformInfo.addRetainConst(innerClassesAttribute.innerClassIndex(i));
                    classTransformInfo.addRetainConst(innerClassesAttribute.innerNameIndex(i));
                }
                break;
            case LocalVariableTable:
                LocalVariableAttribute localVariableAttribute = (LocalVariableAttribute) attributeInfo;
                int lvLen = localVariableAttribute.tableLength();
                for (int i = 0; i < lvLen; i++) {
                    classTransformInfo.addRetainConst(localVariableAttribute.nameIndex(i));
                    classTransformInfo.addRetainConst(localVariableAttribute.descriptorIndex(i));
                }
                break;
            case LocalVariableTypeTable:
                LocalVariableTypeAttribute localVariableTypeAttribute = (LocalVariableTypeAttribute) attributeInfo;
                int lvtLen = localVariableTypeAttribute.tableLength();
                for (int i = 0; i < lvtLen; i++) {
                    classTransformInfo.addRetainConst(localVariableTypeAttribute.nameIndex(i));
                    classTransformInfo.addRetainConst(localVariableTypeAttribute.descriptorIndex(i));
                }
                break;
            case MethodParameters:
                MethodParametersAttribute methodParametersAttribute = (MethodParametersAttribute) attributeInfo;
                int mpSize = methodParametersAttribute.size();
                for (int i = 0; i < mpSize; i++) {
                    classTransformInfo.addRetainConst(methodParametersAttribute.name(i));
                }
                break;
            case NestHost:
                NestHostAttribute nestHostAttribute = (NestHostAttribute) attributeInfo;
                classTransformInfo.addRetainConst(nestHostAttribute.hostClassIndex());
                break;
            case NestMembers:
                NestMembersAttribute nestMembersAttribute = (NestMembersAttribute) attributeInfo;
                int nmLen = nestMembersAttribute.numberOfClasses();
                for (int i = 0; i < nmLen; i++) {
                    classTransformInfo.addRetainConst(nestMembersAttribute.memberClass(i));
                }
                break;
            case Signature:
                SignatureAttribute signatureAttribute = (SignatureAttribute) attributeInfo;
                classTransformInfo.addRetainConst(JavassistExt.signatureAttributeIndex(signatureAttribute));
                break;
            case SourceFile:
                SourceFileAttribute sourceFileAttribute = (SourceFileAttribute) attributeInfo;
                classTransformInfo.addRetainConst(JavassistExt.sourceFileAttributeIndex(sourceFileAttribute));
                break;
            case ConstantValue:
//                ConstantAttribute constantAttribute = (ConstantAttribute) attributeInfo;
                break;
            case Deprecated:
//                DeprecatedAttribute deprecatedAttribute = (DeprecatedAttribute) attributeInfo;
                break;
            case LineNumberTable:
//                LineNumberAttribute lineNumberAttribute = (LineNumberAttribute) attributeInfo;
                // 没有常量池引用
                break;
            case StackMap:
//                StackMap stackMap = (StackMap) attributeInfo;
                break;
            case StackMapTable:
//                StackMapTable stackMapTable = (StackMapTable) attributeInfo;
                break;
            case Synthetic:
//                SyntheticAttribute syntheticAttribute = (SyntheticAttribute) attributeInfo;
                break;
            case RuntimeVisibleTypeAnnotations:
            case RuntimeInvisibleTypeAnnotations:
//                TypeAnnotationsAttribute typeAnnotationsAttribute = (TypeAnnotationsAttribute) attributeInfo;
                // 先不管
                break;
            default:
        }
        return attributeInfo;
    }
}