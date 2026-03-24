package io.kyle.javaguard.constant;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2026/3/24 15:42
 */
public enum RetainFieldAttribute {
//    /**
//     * ConstantValue 单独处理
//     * 仅当字段是 static final 且类型为基本类型 / String 时保留，否则删除
//     */
//    ConstantValue,
    /**
     * 字段的泛型签名（如 List<String>），Jackson 需要
     */
    Signature,
    /**
     * 标记字段已过时，属于 API 契约
     */
    Deprecated,
    /**
     * 字段上的运行时可见注解（如 @Autowired/@JsonProperty），Spring/Jackson 核心依赖
     */
    RuntimeVisibleAnnotations,
    /**
     * 字段上的编译时可见注解，部分工具可能依赖
     */
    RuntimeInvisibleAnnotations,
    /**
     * 字段上的运行时可见类型注解（如 @NonNull String），属于 API 契约
     */
    RuntimeVisibleTypeAnnotations,
    /**
     * 字段上的编译时可见类型注解，部分工具可能依赖
     */
    RuntimeInvisibleTypeAnnotations,

//    可以安全删除	Synthetic	标记合成字段，空壳类不需要
    ;

    public static RetainFieldAttribute from(String str) {
        switch (str) {
//            case "ConstantValue":
//                return ConstantValue;
            case "Signature":
                return Signature;
            case "Deprecated":
                return Deprecated;
            case "RuntimeVisibleAnnotations":
                return RuntimeVisibleAnnotations;
            case "RuntimeInvisibleAnnotations":
                return RuntimeInvisibleAnnotations;
            case "RuntimeVisibleTypeAnnotations":
                return RuntimeVisibleTypeAnnotations;
            case "RuntimeInvisibleTypeAnnotations":
                return RuntimeInvisibleTypeAnnotations;
            default:
                return null;
        }
    }
}
