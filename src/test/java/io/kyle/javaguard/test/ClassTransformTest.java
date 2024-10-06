package io.kyle.javaguard.test;

import io.kyle.javaguard.bean.EncryptInfo;
import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.transform.ClassTransformer;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class ClassTransformTest {
    @Test
    public void test() throws Exception {
        TransformInfo transformInfo = new TransformInfo();
        EncryptInfo encryptInfo = new EncryptInfo("test".getBytes(StandardCharsets.UTF_8));
        transformInfo.setEncrypt(encryptInfo);
        ClassTransformer classTransformer = new ClassTransformer(transformInfo);
        FileInputStream stream = new FileInputStream("D:\\data\\code\\idea\\test_all\\target\\classes\\cn\\java\\test\\TestClass.class");
        FileOutputStream out = new FileOutputStream("out\\TestClass.class");
        classTransformer.encrypt(stream, out);
        stream.close();
        out.close();
    }
}
