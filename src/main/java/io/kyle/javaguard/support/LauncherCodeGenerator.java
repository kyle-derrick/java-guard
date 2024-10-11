package io.kyle.javaguard.support;

import io.kyle.javaguard.bean.EncryptInfo;
import io.kyle.javaguard.bean.SignatureInfo;
import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.constant.ConstVars;
import io.kyle.javaguard.exception.TransformException;
import io.kyle.javaguard.util.BytesUtils;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.NotFoundException;
import javassist.bytecode.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.StringJoiner;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/8 22:29
 */
public class LauncherCodeGenerator {
    private static final Class<?>[] WRITE_RUNTIME_CLASS = {JGTransformInputStream.class};
    private static final String LAUNCHER_CODE_DIR = "jg-launcher";
    private static final String LAUNCHER_CODE_BUILD_CONFIG_FILE = "build_config.rs";
    private static final String LAUNCHER_BUILD_PATH = "build";
    private static final String LAUNCHER_CODE_BUILD_CONFIG_PATH = LAUNCHER_BUILD_PATH + File.separatorChar + LAUNCHER_CODE_BUILD_CONFIG_FILE;
    private static final String LAUNCHER_CLASS_DIR_PATH = LAUNCHER_BUILD_PATH + File.separatorChar + "class";
    private static final String LAUNCHER_RUNTIME_CLASS_FILE = "runtime.classes";
    private static final String LAUNCHER_TRANSFORM_MOD_FILE = "transform.mod";
    public static void generate(String output, TransformInfo info) throws TransformException {
        File launcherDir = new File(output, LAUNCHER_CODE_DIR);
        if (!launcherDir.exists()) {
            launcherDir.mkdirs();
        }
        // todo copy to launcher dir
        generateBuildConfigRs(launcherDir, info);
        generateClass(launcherDir, info);
    }

    private static void generateBuildConfigRs(File launcherDir, TransformInfo info) throws TransformException {
        EncryptInfo encrypt = info.getEncrypt();
        SignatureInfo signatureInfo = info.getSignature();
        String content;
        try (InputStream configRs = LauncherCodeGenerator.class.getClassLoader().getResourceAsStream(LAUNCHER_CODE_BUILD_CONFIG_FILE)) {
            content = IOUtils.toString(configRs, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new TransformException("read launcher build config failed", e);
        }
        HashMap<String, String> valueMap = new HashMap<>(4);
        valueMap.put("key", bytesToString(encrypt.getKey()));
        valueMap.put("resourceKey", bytesToString(encrypt.getResourceKey()));
        valueMap.put("publicKey", bytesToString(signatureInfo.getPublicKey()));
        valueMap.put("signKeyVersion", signatureInfo.getKeyHash());
        content = new StringSubstitutor(valueMap).replace(content);
        try {
            FileUtils.write(new File(launcherDir, LAUNCHER_CODE_BUILD_CONFIG_PATH), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new TransformException("write launcher build config failed", e);
        }
    }

    private static void generateClass(File launcherDir, TransformInfo info) throws TransformException {
        File launcherClassDir = new File(launcherDir, LAUNCHER_CLASS_DIR_PATH);
        if (!launcherClassDir.exists()) {
            launcherClassDir.mkdirs();
        }
        EncryptInfo encrypt = info.getEncrypt();
        String code = null;
        try {
            code = URLExtCode.format(ClassDecryption.URL_OPEN_CONNECTION_RENAME, ConstVars.ENCRYPT_RESOURCE_HEADER,
                    encrypt.getResourceKey(), encrypt.getAlgorithm(), encrypt.getTransformation());
        } catch (IOException e) {
            throw new TransformException("generate resource decrypt code failed", e);
        }
        ClassPool pool = ClassPool.getDefault();
        CtClass classDecryptionClass = null;
        CtField urlOpenConnectionCode;
        try {
            classDecryptionClass = pool.getCtClass(ClassDecryption.class.getName());
            urlOpenConnectionCode = classDecryptionClass.getDeclaredField("URL_OPEN_CONNECTION_CODE");
        } catch (NotFoundException e) {
            throw new TransformException("handle resource decrypt class failed", e);
        }
        ConstPool constPool = classDecryptionClass.getClassFile().getConstPool();
        byte[] encryptCode = null;
        try {
            encryptCode = encrypt.encrypt(code.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new TransformException("encrypt resource decrypt code failed", e);
        }
        int index = constPool.addUtf8Info(Base64.getEncoder().withoutPadding().encodeToString(encryptCode));
        byte[] constValue = urlOpenConnectionCode.getAttribute(ConstantAttribute.tag);
        ByteArray.write16bit(index, constValue, 0);

        // transform mod, class and javassist
        File classesFile = new File(launcherClassDir, LAUNCHER_TRANSFORM_MOD_FILE);
        try (OutputStream out = new CipherOutputStream(Files.newOutputStream(classesFile.toPath()), encrypt.getCipher(Cipher.ENCRYPT_MODE))) {
            byte[] bytecode = classDecryptionClass.toBytecode();
            out.write(BytesUtils.intToBytes(bytecode.length));
            out.write(bytecode);
            URL javassist = ClassFile.class.getProtectionDomain().getCodeSource().getLocation();
            int length = javassist.getFile().length();
            out.write(BytesUtils.intToBytes(length));
            try (InputStream javassistIn = javassist.openStream()) {
                IOUtils.copy(javassistIn, out);
            }
        } catch (Exception e) {
            throw new TransformException("write classes failed", e);
        }

        // runtime classes
        File out = new File(launcherClassDir, LAUNCHER_RUNTIME_CLASS_FILE);
        try (OutputStream outputStream = new CipherOutputStream(Files.newOutputStream(out.toPath()), encrypt.getCipher(Cipher.ENCRYPT_MODE))) {
            for (Class<?> clazz : WRITE_RUNTIME_CLASS) {
                try (InputStream stream = clazz.getResourceAsStream(clazz.getSimpleName() + ".class")) {
                    byte[] name = clazz.getName().getBytes(StandardCharsets.UTF_8);
                    assert stream != null;
                    byte[] byteArray = IOUtils.toByteArray(stream);
                    outputStream.write(BytesUtils.intToBytes(name.length));
                    outputStream.write(name);
                    outputStream.write(BytesUtils.intToBytes(byteArray.length));
                    outputStream.write(byteArray);
                    IOUtils.copy(stream, outputStream);
                }
            }
        } catch (Exception e) {
            throw new TransformException("write runtime class failed", e);
        }

        // javassist jar
    }

    private static String bytesToString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "[]";
        }
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (byte b : bytes) {
            joiner.add(Byte.toUnsignedInt(b) + "u8");
        }
        return joiner.toString();
    }
}
