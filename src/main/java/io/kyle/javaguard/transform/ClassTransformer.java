package io.kyle.javaguard.transform;

import io.kyle.javaguard.bean.ClassRequireFieldInfo;
import io.kyle.javaguard.bean.ClassRequiredInfos;
import io.kyle.javaguard.bean.ClassTransformInfo;
import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.constant.ClassAttribute;
import io.kyle.javaguard.constant.ConstVars;
import io.kyle.javaguard.constant.PrimitiveType;
import io.kyle.javaguard.exception.TransformException;
import io.kyle.javaguard.exception.TransformRuntimeException;
import io.kyle.javaguard.util.ClassFileUtils;
import javassist.Modifier;
import javassist.bytecode.*;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.AnnotationExt;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/9/30 14:17
 */
public class ClassTransformer extends AbstractTransformer {
    @SuppressWarnings("unused")
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
        ClassFile classFile;
        try {
            classFile = new ClassFile(new DataInputStream(in));
        } catch (IOException e) {
            throw new TransformException("analysis class byte failed", e);
        }
        ConstPool constPool = classFile.getConstPool();
        ClassTransformInfo classTransformInfo = new ClassTransformInfo(constPool);
        Map<String, ClassRequireFieldInfo> fieldMap = new HashMap<>();
        int staticCount = 0;
        for (FieldInfo field : classFile.getFields()) {
            classTransformInfo.addRetainConst(JavassistExt.fieldNameIndex(field));
            classTransformInfo.addRetainConst(JavassistExt.fieldDescriptorIndex(field));
            for (AttributeInfo attribute : field.getAttributes()) {
                handleAttribute(attribute, classTransformInfo);
            }
            int accessFlags;
            if (field.getConstantValue() == 0 && (AccessFlag.FINAL & (accessFlags = field.getAccessFlags())) != 0) {
                boolean isStatic = Modifier.isStatic(accessFlags);
                if (isStatic) {
                    staticCount++;
                }
                fieldMap.put(field.getName() + ' ' + field.getDescriptor(), new ClassRequireFieldInfo(field.getName(), field.getDescriptor(), isStatic));
            }
        }

        ClassRequiredInfos requiredInfos = ClassFileUtils.requiredInfos(classFile, fieldMap, staticCount);

        for (MethodInfo method : classFile.getMethods()) {
            handleMethod(classFile, method, classTransformInfo, requiredInfos);
        }

        // 疑似重复
//        MethodInfo staticInitializer = classFile.getStaticInitializer();
//        handleMethod(classFile, staticInitializer, classTransformInfo, requiredInfos);

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
            DataOutputStream stream = new DataOutputStream(out);
            classFile.write(stream);
            stream.flush();
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

    protected void handleMethod(ClassFile classFile, MethodInfo method, ClassTransformInfo classTransformInfo,
                                ClassRequiredInfos requiredInfos) {
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
                attributeInfo = handleCodeAttribute(classFile, method, (CodeAttribute) attribute, classTransformInfo,
                        method.getDescriptor(), requiredInfos);
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

    protected CodeAttribute handleCodeAttribute(ClassFile classFile, MethodInfo method, CodeAttribute codeAttribute,
                                                ClassTransformInfo classTransformInfo, String descriptor,
                                                ClassRequiredInfos requiredInfos) {
        if (codeAttribute == null) {
            return null;
        }
        for (AttributeInfo codeAttributeAttribute : codeAttribute.getAttributes()) {
            handleAttribute(codeAttributeAttribute, classTransformInfo);
        }
        if (ClassFileUtils.isEmptyCode(codeAttribute)) {
            classTransformInfo.addCode(null);
            return codeAttribute;
        }

        boolean notStatic = (method.getAccessFlags() & AccessFlag.STATIC) == 0;
        int locals = Descriptor.numOfParameters(method.getDescriptor());
        if (notStatic) {
            locals++;
        }

        Bytecode bytecode = defaultCodeGenerate(classFile, method, codeAttribute, requiredInfos);
        CodeAttribute newCodeAttribute = new CodeAttribute(codeAttribute.getConstPool(),
                bytecode.getMaxStack(), locals, bytecode.get(), bytecode.getExceptionTable());
//        newCodeAttribute.getAttributes().addAll(codeAttribute.getAttributes());
        ClassFileUtils.codeInnerAttributeHandle(bytecode.length(), codeAttribute, newCodeAttribute, method.getConstPool());
        // 暂不需要index了，解密时直接按序解密
//        newCodeAttribute.getAttributes()
//                .add(new CodeIndexAttribute(codeAttribute.getConstPool(), classTransformInfo.codesSize()));
        classTransformInfo.addCode(codeAttribute);
        return newCodeAttribute;
    }

    private Bytecode defaultCodeGenerate(ClassFile classFile, MethodInfo method, CodeAttribute codeAttribute,
                                     ClassRequiredInfos requiredInfos) {
        ConstPool constPool = method.getConstPool();
        Bytecode newCodeBytes = new Bytecode(constPool);
        switch (method.getName()) {
            case ConstVars.CONSTRUCTOR_METHOD_NAME:
                int constructorIndex = ClassFileUtils.constructorSuperInvokeMethodRef(classFile, constPool, codeAttribute);
                if (constructorIndex == 0) {
                    System.out.println("WARN: not found init instruction in constructor: " + classFile.getName() + ':' + method.getName());
                } else {
                    String desc = constPool.getMethodrefType(constructorIndex);
                    PrimitiveType[] paramTypes = PrimitiveType.paramTypes(desc);
                    newCodeBytes.addOpcode(Opcode.ALOAD_0);
                    for (PrimitiveType parameterType : paramTypes) {
                        if (parameterType.defaultValue != Opcode.NOP) {
                            newCodeBytes.addOpcode(parameterType.defaultValue);
                        }
                    }
                    newCodeBytes.addOpcode(Opcode.INVOKESPECIAL);
                    newCodeBytes.addIndex(constructorIndex);
                    ClassFileUtils.putCode(requiredInfos.fields, Opcode.PUTFIELD, newCodeBytes);
                }
                newCodeBytes.addOpcode(Opcode.RETURN);
                break;
            case ConstVars.STATIC_BLOCK_METHOD_NAME:
                ClassFileUtils.putCode(requiredInfos.staticFields, Opcode.PUTSTATIC, newCodeBytes);
                newCodeBytes.addOpcode(Opcode.RETURN);
                break;
            default:
                PrimitiveType returnType = PrimitiveType.returnType(method.getDescriptor());
                if (returnType.defaultValue != Opcode.NOP) {
                    newCodeBytes.addOpcode(returnType.defaultValue);
                }
                newCodeBytes.addOpcode(returnType.returnOpcode);
        }
        return newCodeBytes;
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
                // codeAttribute ignore, please use handleCodeAttribute
                return attributeInfo;
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