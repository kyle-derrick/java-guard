package io.kyle.javaguard.test;

import io.kyle.javaguard.bean.EncryptInfo;
import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.transform.ClassTransformer;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.ClassFile;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ClassTransformTest {
    @Test
    public void test() throws Exception {
        InputStream origin = ClassTransformTest.class.getResourceAsStream("TestClass.class");
        Files.createDirectories(Paths.get("out/e"));
        Files.createDirectories(Paths.get("out/d"));
        String outEncryptFile = "out/e/TestClass.class";
        String outDecryptFile = "out/d/TestClass.class";
        TransformInfo transformInfo = new TransformInfo();
        EncryptInfo encryptInfo = new EncryptInfo(DigestUtils.sha256("test"));
        transformInfo.setEncrypt(encryptInfo);
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

    @Test
    public void test2() throws Exception {
//        byte[] bytes = FileUtils.readFileToByteArray(new File("/home/kyle/data/code/java/JavaGuard/out/e/TestClass.class"));
//        ClassFile classFile = new ClassFile(new DataInputStream(new ByteArrayInputStream(bytes)));
//        CtClass ctClass = ClassPool.getDefault().makeClass(classFile);
//        Class<?> aClass = ctClass.toClass();
        Class<?> aClass = ClassLoader.getSystemClassLoader().loadClass("io.kyle.javaguard.test.TestClass");

        Method code2 = aClass.getMethod("code2", byte[].class);
        LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();
        String[] parameterNames = discoverer.getParameterNames(code2);
        System.out.println(String.join(",", parameterNames));
    }
}
