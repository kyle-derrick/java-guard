package io.kyle.javaguard.transform;

import io.kyle.javaguard.bean.ClassTransformInfo;
import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.constant.ClassAttribute;
import javassist.bytecode.*;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.AnnotationExt;
import org.apache.commons.compress.utils.ByteUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.ListIterator;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/9/30 14:17
 */
public class ClassTransformer extends AbstractTransformer {

    public ClassTransformer(TransformInfo transformInfo) {
        super(transformInfo);
    }

    @Override
    public boolean isSupport(String name) {
        return StringUtils.endsWith(name, ".class");
    }

    @Override
    public boolean encrypt(InputStream in, OutputStream out) throws IOException {
        ClassFile classFile = new ClassFile(new DataInputStream(in));
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
        classFile.addAttribute(new SecretBoxAttribute(constPool, bytes));

        classFile.write(new DataOutputStream(out));
        return true;
    }

    @Override
    public boolean decrypt(InputStream in, OutputStream out) throws IOException {

        return true;
    }

    protected void handleMethod(MethodInfo method, ClassTransformInfo classTransformInfo) {
        classTransformInfo.addRetainConst(JavassistExt.methodNameIndex(method));
        classTransformInfo.addRetainConst(JavassistExt.methodDescriptorIndex(method));
        ListIterator<AttributeInfo> iterator = method.getAttributes().listIterator();
        while (iterator.hasNext()) {
            AttributeInfo attribute = iterator.next();
            AttributeInfo attributeInfo = handleAttribute(attribute, classTransformInfo);
            if (attributeInfo != attribute) {
                iterator.set(attributeInfo);
            }
        }
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
                for (AttributeInfo codeAttributeAttribute : codeAttribute.getAttributes()) {
                    handleAttribute(codeAttributeAttribute, classTransformInfo);
                }
//                codeAttribute.getExceptionTable()
                // todo 看需不需要处理ExceptionTable

                CodeAttribute newCodeAttribute = new CodeAttribute(codeAttribute.getConstPool(),
                        codeAttribute.getMaxStack(), codeAttribute.getMaxLocals(), ByteUtils.EMPTY_BYTE_ARRAY,
                        codeAttribute.getExceptionTable());
                newCodeAttribute.getAttributes().addAll(codeAttribute.getAttributes());
                newCodeAttribute.getAttributes()
                        .add(new CodeIndexAttribute(codeAttribute.getConstPool(), classTransformInfo.codesSize()));
                classTransformInfo.addCode(codeAttribute.getCode());
                return newCodeAttribute;
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