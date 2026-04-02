package io.kyle.javaguard.test;

import io.kyle.javaguard.bean.KeyInfo;
import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.transform.ClassTransformer;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

@Ignore
public class ClassTransformTest {
    @Test
    public void test() throws Exception {
        InputStream origin = ClassTransformTest.class.getResourceAsStream("TestClass.class");
        Files.createDirectories(Paths.get("out/e"));
        Files.createDirectories(Paths.get("out/d"));
        String outEncryptFile = "out/e/TestClass.class";
        String outDecryptFile = "out/d/TestClass.class";
        TransformInfo transformInfo = new TransformInfo();
        byte[] sha512 = DigestUtils.sha512("test");
        transformInfo.setKeyInfo(new KeyInfo(Arrays.copyOfRange(sha512, 0, 512 >> 4)));
        transformInfo.setResourceKeyInfo(new KeyInfo(Arrays.copyOfRange(sha512, 512 >> 4, sha512.length)));
        ClassTransformer classTransformer = new ClassTransformer(transformInfo);
        BufferedInputStream stream = new BufferedInputStream(origin);
        FileOutputStream out = new FileOutputStream(outEncryptFile);
        classTransformer.encrypt(stream, out);
        stream.close();
        out.close();
        FileInputStream encryptStream = new FileInputStream(outEncryptFile);
        FileOutputStream outDecryptStream = new FileOutputStream(outDecryptFile);
        classTransformer.decrypt(encryptStream, outDecryptStream);
        encryptStream.close();
        outDecryptStream.close();
    }
}