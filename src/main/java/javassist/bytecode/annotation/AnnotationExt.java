package javassist.bytecode.annotation;

import io.kyle.javaguard.bean.ClassTransformInfo;

import java.util.function.BiConsumer;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/08 10:32
 */
public class AnnotationExt {
    public static int annotationAttributeTypeIndex(Annotation annotation) {
        return annotation.typeIndex;
    }
    public static void foreachAnnotationAttributeMembers(Annotation annotation, BiConsumer<Integer, MemberValue> handle) {
        if (annotation.members == null) {
            return;
        }
        annotation.members.forEach((name, pair) -> handle.accept(pair.name, pair.value));
    }

    public static void retainAnnotation(Annotation annotation, ClassTransformInfo classTransformInfo) {
        if (annotation == null) {
            return;
        }
        classTransformInfo.addRetainConst(annotation.typeIndex);
        foreachAnnotationAttributeMembers(annotation, (nameIndex, memberValue) -> {
            classTransformInfo.addRetainConst(nameIndex);
            retainMembers(memberValue, classTransformInfo);
        });
    }

    public static void retainMembers(MemberValue memberValue, ClassTransformInfo classTransformInfo) {
        if (memberValue == null) {
            return;
        }
        try {
            switch (memberValue.tag) {
                case '@':
                    AnnotationMemberValue annotation = (AnnotationMemberValue) memberValue;
                    Annotation value = annotation.getValue();
                    retainAnnotation(value, classTransformInfo);
                    break;
                case '[':
                    ArrayMemberValue arrayMemberValue = (ArrayMemberValue) memberValue;
                    retainMembers(arrayMemberValue.getType(), classTransformInfo);
                    MemberValue[] array = arrayMemberValue.getValue();
                    if (array != null) {
                        for (MemberValue item : array) {
                            retainMembers(item, classTransformInfo);
                        }
                    }
                    break;
                case 'c':
                    classTransformInfo.addRetainConst(((ClassMemberValue) memberValue).valueIndex);
                    break;
                case 'e':
                    EnumMemberValue enumMember = (EnumMemberValue) memberValue;
                    classTransformInfo.addRetainConst((enumMember).typeIndex);
                    classTransformInfo.addRetainConst((enumMember).valueIndex);
                    break;
                case 'Z':
                    classTransformInfo.addRetainConst(((BooleanMemberValue) memberValue).valueIndex);
                    break;
                case 'B':
                    classTransformInfo.addRetainConst(((ByteMemberValue) memberValue).valueIndex);
                    break;
                case 'C':
                    classTransformInfo.addRetainConst(((CharMemberValue) memberValue).valueIndex);
                    break;
                case 'D':
                    classTransformInfo.addRetainConst(((DoubleMemberValue) memberValue).valueIndex);
                    break;
                case 'F':
                    classTransformInfo.addRetainConst(((FloatMemberValue) memberValue).valueIndex);
                    break;
                case 'I':
                    classTransformInfo.addRetainConst(((IntegerMemberValue) memberValue).valueIndex);
                    break;
                case 'J':
                    classTransformInfo.addRetainConst(((LongMemberValue) memberValue).valueIndex);
                    break;
                case 'S':
                    classTransformInfo.addRetainConst(((ShortMemberValue) memberValue).valueIndex);
                    break;
                case 's':
                    classTransformInfo.addRetainConst(((StringMemberValue) memberValue).valueIndex);
                    break;
                default:
            }
        } catch (Exception e) {
            System.err.println("WARN: error when retain member values: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
