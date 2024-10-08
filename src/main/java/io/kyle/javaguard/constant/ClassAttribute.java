package io.kyle.javaguard.constant;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/08 10:32
 */
public enum ClassAttribute {
    /**
     *
     */
    AnnotationDefault,
    RuntimeVisibleAnnotations,
    RuntimeInvisibleAnnotations,
    BootstrapMethods,
    Code,
    ConstantValue,
    Deprecated,
    EnclosingMethod,
    Exceptions,
    InnerClasses,
    LineNumberTable,
    LocalVariableTable,
    LocalVariableTypeTable,
    MethodParameters,
    NestHost,
    NestMembers,
    Signature,
    SourceFile,
    StackMap,
    StackMapTable,
    Synthetic,
    RuntimeVisibleTypeAnnotations,
    RuntimeInvisibleTypeAnnotations,
    ;

    public static ClassAttribute of(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
