package io.kyle.javaguard.constant;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2026/3/24 15:42
 */
public enum RetainCodeAttribute {
    /**
     * 仅当原类有且没有 MethodParameters 时保留，仅保留方法参数的部分，用于 Spring MVC/Swagger 获取参数名	框架需要参数名
     */
    LocalVariableTable,
//    exception_table,//可以安全删除	空方法不需要异常表	框架不读
//    StackMapTable,//可以安全删除	JDK6+ 类型检查栈图，空方法不需要	框架不读
//    LineNumberTable,//可以安全删除	行号表，调试用	框架不读
//    LocalVariableTypeTable,//可以安全删除	局部变量泛型表，调试用	框架不读
//    RuntimeVisibleTypeAnnotations,//可以安全删除	方法体里的运行时可见类型注解，空方法无实现	框架不读
//    RuntimeInvisibleTypeAnnotations,//可以安全删除	方法体里的编译时可见类型注解，空方法无实现	框架不读
    ;

    public static RetainCodeAttribute from(String str) {
        switch (str) {
            case "LocalVariableTable":
                return LocalVariableTable;
            default:
                return null;
        }
    }
}
