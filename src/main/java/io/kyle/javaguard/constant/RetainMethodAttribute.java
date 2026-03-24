package io.kyle.javaguard.constant;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2026/3/24 15:42
 */
public enum RetainMethodAttribute {
    /**
     * 方法的异常声明（如 throws IOException），属于 API 签名
     */
    Exceptions,
    /**
     * 方法的泛型签名（如 <T> T get(T t)），Jackson/MyBatis 需要
     */
    Signature,
    /**
     * 标记方法已过时，属于 API 契约
     */
    Deprecated,
    /**
     * 方法上的运行时可见注解（如 @RequestMapping/@Select），Spring/MyBatis 核心依赖
     */
    RuntimeVisibleAnnotations,
    /**
     * 方法上的编译时可见注解，部分工具可能依赖
     */
    RuntimeInvisibleAnnotations,
    /**
     * 方法参数上的运行时可见注解（如 @RequestParam/@Param），Spring/MyBatis 核心依赖
     */
    RuntimeVisibleParameterAnnotations,
    /**
     * 方法参数上的编译时可见注解，部分工具可能依赖
     */
    RuntimeInvisibleParameterAnnotations,
    /**
     * 方法上的运行时可见类型注解，属于 API 契约
     */
    RuntimeVisibleTypeAnnotations,
    /**
     * 方法上的编译时可见类型注解，部分工具可能依赖
     */
    RuntimeInvisibleTypeAnnotations,
    /**
     * 注解类方法（注解元素）的默认值，删了注解无法使用
     */
    AnnotationDefault,
    /**
     * JDK8+ 方法参数名（编译时加 -parameters），Spring MVC/Swagger 核心依赖
     */
    MethodParameters,
    /**
     * 标记桥接方法（泛型擦除生成），保留其注解，框架可能需要
     */
    Bridge,

//    /**
//     * Code 单独处理
//     * 非 abstract/native 方法必须有，内部仅保留 max_stack、max_locals、code 数组（见下文 Code_attribute 层级）
//     */
//    Code,

//    可以安全删除	Synthetic	标记合成方法，仅当合成方法有注解时保留，否则删除
    ;


    public static RetainMethodAttribute from(String str) {
        switch (str) {
            case "Exceptions":
                return Exceptions;
            case "Signature":
                return Signature;
            case "Deprecated":
                return Deprecated;
            case "RuntimeVisibleAnnotations":
                return RuntimeVisibleAnnotations;
            case "RuntimeInvisibleAnnotations":
                return RuntimeInvisibleAnnotations;
            case "RuntimeVisibleParameterAnnotations":
                return RuntimeVisibleParameterAnnotations;
            case "RuntimeInvisibleParameterAnnotations":
                return RuntimeInvisibleParameterAnnotations;
            case "RuntimeVisibleTypeAnnotations":
                return RuntimeVisibleTypeAnnotations;
            case "RuntimeInvisibleTypeAnnotations":
                return RuntimeInvisibleTypeAnnotations;
            case "AnnotationDefault":
                return AnnotationDefault;
            case "MethodParameters":
                return MethodParameters;
            case "Bridge":
                return Bridge;
            default:
                return null;
        }
    }
}
