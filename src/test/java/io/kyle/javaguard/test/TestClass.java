package io.kyle.javaguard.test;

import java.io.IOException;
import java.lang.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@TestClass.MyAnnotation(22)
@TestClass.MyAnnotation2
public class TestClass<@TestClass.MyTypeAnnotation("asd") T> implements Consumer<TestClass> {
    private static final byte[] x = "asdafadsasdasdfasdgadf".getBytes(StandardCharsets.UTF_8);
    private static final byte[] x2 = {1,2,3,4,5};
    private static final String T1 = "asfd";
    private static String T2 = "asfd2";
    private static String T3;
    private final int t1 = 2;
    @MyAnnotation(2)
    private int t2 = 3;
    private int t3;

    @MyAnnotation(12)
    public TestClass(int t2) {
        this.t2 = t2;
    }

    @MyAnnotation(value = 122, name = "asdasd")
    public static void test() {

    }
    public static boolean testb() {
        return false;
    }
    public static char testc() {
        return 0;
    }
    public static byte testby() {
        return 0;
    }
    public static short testbs() {
        return 0;
    }
    public static int testi() {
        return 0;
    }
    public static float testf() {
        return 0;
    }
    public static long testl() {
        return 0;
    }
    public static double testd() {
        return 0;
    }

    public Object testr() {
        byte[] x = "asdafadsfasdgadf".getBytes(StandardCharsets.UTF_8);
        return null;
    }

    public static native byte[] code1(byte[] code);

    public native byte[] code2(byte[] code);

    public static void main(String[] args) throws IOException, RuntimeException {
        Consumer<Object> x = (i) -> System.out.println(i);
    }

    @Override
    public void accept(TestClass o) {
        try {
            int i = 1/0;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
    static @interface MyAnnotation {
        int value();
        String name() default "asda";
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
    static @interface MyAnnotation2 {
        String value() default "qwe";
    }

    @Target({ElementType.TYPE_USE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MyTypeAnnotation {
        String value();
    }

    public static enum TestEnum {
        /**
         *
         */
        A(1) {
            @Override
            public void t() {
                System.out.println();
            }
        },
        B(1) {
            @Override
            public void t() {
                System.out.println();
            }
        },
        C(1) {
            @Override
            public void t() {
                System.out.println();
            }
        },
        ;
        private final int i;

        TestEnum(int i) {
            this.i = i;
        }

        public abstract void t();
    }
}
