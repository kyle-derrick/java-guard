package io.kyle.javaguard.constant;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2026/3/24 15:42
 */
public enum RetainClassAttribute {
    /**
     * 框架需要正确识别内部类 / 宿主类的关系
     */
    InnerClasses,
    /**
     * JDK11+ 内部类的宿主类标识，删了内部类无法加载
     */
    NestHost,
    /**
     * JDK11+ 宿主类的嵌套成员列表，保留内部类时必须有
     */
    NestMembers,
    /**
     * 类的泛型签名（如 class User<T extends Base>），Jackson/MyBatis 需要
     */
    Signature,

    /**
     * 标记类已过时，属于 API 契约
     */
    Deprecated,
    /**
     * 类上的运行时可见注解（如 @Service/@Controller），Spring 核心依赖
     */
    RuntimeVisibleAnnotations,

    /**
     * 类上的编译时可见注解（如 @SuppressWarnings），部分工具可能依赖
     */
    RuntimeInvisibleAnnotations,
    /**
     * JDK16+ 记录类的核心标识，包含组件信息，删了报 ClassFormatError
     */
    Record,

    /**
     * JDK17+ 密封类的允许子类列表，删了报 ClassFormatError
     */
    PermittedSubclasses,

    /**
     * JDK9+ 模块类的核心声明，包含导出 / 打开 /requires 等，删了模块无法加载
     */
    Module,
    /**
     * JDK9+ 模块导出的包列表，保留 Module 时必须有
     */
    ModulePackages,
    /**
     * JDK9+ 模块的主类（如果原类有），保留 Module 时建议保留
     */
    ModuleMainClass,
//    -------------------------------------------------------------
//    以下为非必保留
//
//    /**
//     * 建议保留
//     * 源文件名，报错信息更友好
//     */
//    SourceFile,
//    /**
//     * 可以安全删除
//     * 调试扩展信息，框架不需要
//     */
//    SourceDebugExtension,
//    /**
//     * 可以安全删除
//     * 标记合成类（编译器生成），空壳类不需要
//     */
//    Synthetic,
//    /**
//     * 可以安全删除
//     * JDK7+ invokedynamic 的引导方法，空壳类无实现
//     */
//    BootstrapMethods,
//    /**
//     * 可以安全删除
//     * 匿名内部类 / 局部内部类的宿主方法，空壳类一般不保留这些
//     */
//    EnclosingMethod,
//    /**
//     * 可以安全删除
//     * JDK9+ 编译 ID，调试用
//     */
//    CompilationID,
//    /**
//     * 可以安全删除
//     * JDK9+ 源文件 ID，调试用
//     */
//    SourceID,
//    /**
//     * 可以安全删除
//     * 类上的编译时可见类型注解，框架不需要
//     */
//    RuntimeInvisibleTypeAnnotations,
    ;

    public static RetainClassAttribute from(String str) {
        switch (str) {
            case "InnerClasses":
                return InnerClasses;
            case "NestHost":
                return NestHost;
            case "NestMembers":
                return NestMembers;
            case "Signature":
                return Signature;
            case "Deprecated":
                return Deprecated;
            case "RuntimeVisibleAnnotations":
                return RuntimeVisibleAnnotations;
            case "RuntimeInvisibleAnnotations":
                return RuntimeInvisibleAnnotations;
            case "Record":
                return Record;
            case "PermittedSubclasses":
                return PermittedSubclasses;
            case "Module":
                return Module;
            case "ModulePackages":
                return ModulePackages;
            case "ModuleMainClass":
                return ModuleMainClass;
            default:
                return null;
        }
    }
}
