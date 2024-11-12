package io.kyle.javaguard.test;

import io.kyle.javaguard.support.URLExtCode;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.ClassFile;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.net.URL;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/8 9:31
 */
@Ignore
public class ClassGenerateTest {
    private static final byte[] ENCRYPT_RESOURCE_HEADER = {0, 74, 71, 82, 0};
    private static final String URL_OPEN_CONNECTION_NAME = "openConnection";
    private static final String URL_OPEN_CONNECTION_RENAME = "_openConnection_";
    @Test
    public void test() throws Exception {
        String code = URLExtCode.format(URL_OPEN_CONNECTION_RENAME, ENCRYPT_RESOURCE_HEADER, DigestUtils.sha256("asd"), "AES", "AES/GCM/NoPadding");

        ClassPool pool = ClassPool.getDefault();
        CtClass ctClass = pool.getCtClass(URL.class.getName());
        byte[] data = ctClass.toBytecode();
//        byte[] data;
//        try (InputStream resource = ClassGenerateTest.class.getResourceAsStream("/URL.class")) {
//            data = IOUtils.toByteArray(resource);
//        }
        ClassFile urlClassFile = new ClassFile(new DataInputStream(new ByteArrayInputStream(data)));
        CtClass urlClass = pool.makeClass(urlClassFile, false);
        CtMethod openConnection = urlClass.getDeclaredMethod(URL_OPEN_CONNECTION_NAME);
        openConnection.setName(URL_OPEN_CONNECTION_RENAME);
        CtMethod newOpenConnection = new CtMethod(openConnection.getReturnType(), URL_OPEN_CONNECTION_NAME, new CtClass[0], urlClass);
        newOpenConnection.setBody(code);
        urlClass.addMethod(newOpenConnection);
        byte[] bytecode = urlClass.toBytecode();
        FileUtils.writeByteArrayToFile(new File("out/URL.class"), bytecode);
    }
}
