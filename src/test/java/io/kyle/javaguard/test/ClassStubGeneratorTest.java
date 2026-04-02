package io.kyle.javaguard.test;

import io.kyle.javaguard.util.ClassStubGenerator;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Ignore
public class ClassStubGeneratorTest {

    /**
     * 验证对 TestClass 执行空壳化后：
     * 1. 生成的字节码不为空；
     * 2. 可以被 ClassLoader 正常加载（字节码合法）；
     * 3. stub class 文件比原始 class 文件更小（方法体被清空）。
     */
    @Test
    public void testGenerateStubClass() throws Exception {
        String resourcePath = "/" + TestClass.class.getName().replace('.', '/') + ".class";
        byte[] originalBytes = readResource(resourcePath);
        Assert.assertNotNull("Test resource not found: " + resourcePath, originalBytes);

        byte[] stubBytes = ClassStubGenerator.generateStubClass(originalBytes, null);

        Assert.assertNotNull("generateStubClass returned null", stubBytes);
        Assert.assertTrue("Stub bytes should not be empty", stubBytes.length > 0);

        // 加载验证：stub 字节码必须合法可加载
        Class<?> stubClass = new IsolatedClassLoader().defineClass(TestClass.class.getName(), stubBytes);
        Assert.assertEquals(TestClass.class.getName(), stubClass.getName());

        System.out.println("Original size : " + originalBytes.length + " bytes");
        System.out.println("Stub size     : " + stubBytes.length + " bytes");

        Assert.assertTrue("Stub should be smaller than original (method bodies cleared)",
                stubBytes.length < originalBytes.length);
    }

    /**
     * 验证对多个不同类型的类执行空壳化不会抛异常（烟测试）。
     */
    @Test
    public void testGenerateStubForVariousClasses() throws Exception {
        Class<?>[] targets = {
                TestClass.class,
                TestClass.TestEnum.class,
        };
        for (Class<?> target : targets) {
            String resourcePath = "/" + target.getName().replace('.', '/') + ".class";
            byte[] originalBytes = readResource(resourcePath);
            if (originalBytes == null) {
                System.out.println("SKIP: " + resourcePath + " not accessible as resource");
                continue;
            }
            byte[] stubBytes = ClassStubGenerator.generateStubClass(originalBytes, null);
            Assert.assertNotNull("generateStubClass returned null for " + target.getName(), stubBytes);
            Assert.assertTrue("Stub bytes empty for " + target.getName(), stubBytes.length > 0);
            System.out.println("OK: " + target.getName()
                    + " original=" + originalBytes.length + " stub=" + stubBytes.length);
        }
    }

    // -------------------------------------------------------------------------

    private static byte[] readResource(String resourcePath) throws Exception {
        InputStream is = ClassStubGeneratorTest.class.getResourceAsStream(resourcePath);
        if (is == null) {
            return null;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = is.read(buffer)) != -1) {
                baos.write(buffer, 0, n);
            }
            return baos.toByteArray();
        } finally {
            is.close();
        }
    }

    /** 独立 ClassLoader，避免与系统 ClassLoader 冲突。 */
    private static class IsolatedClassLoader extends ClassLoader {
        IsolatedClassLoader() {
            super(null);
        }

        Class<?> defineClass(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }
}
