package javassist.bytecode.annotation;

import java.util.function.BiConsumer;

public class AnnotationExt {
    public static int annotationAttributeTypeIndex(Annotation annotation) {
        return annotation.typeIndex;
    }
    public static void foreachAnnotationAttributeMembers(Annotation annotation, BiConsumer<Integer, MemberValue> handle) {
        if (annotation.members == null) {
            return;
        }
        annotation.members.forEach((name, pair) -> {
            handle.accept(pair.name, pair.value);
        });
    }

}
