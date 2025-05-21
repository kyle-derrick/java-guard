package io.kyle.javaguard.support;

import io.kyle.javaguard.bean.KeyInfo;
import io.kyle.javaguard.bean.SignatureInfo;
import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.exception.TransformException;
import io.kyle.javaguard.util.BytesUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.StringJoiner;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/8 22:29
 */
public class LauncherCodeGenerator {
    private static final Class<?>[] WRITE_RUNTIME_CLASS = {InternalResourceDecryptInputStream.class, InternalResourceURLConnection.class};
    private static final String LAUNCHER_CODE_DIR = "jg-launcher";
    private static final String LAUNCHER_CODE_BUILD_CONFIG_FILE = "build_config.rs";
    private static final String LAUNCHER_BUILD_PATH = "build";
    private static final String LAUNCHER_CODE_BUILD_CONFIG_PATH = LAUNCHER_BUILD_PATH + File.separatorChar + LAUNCHER_CODE_BUILD_CONFIG_FILE;
    private static final String LAUNCHER_CLASS_DIR_PATH = LAUNCHER_BUILD_PATH + File.separatorChar + "ext";
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
        KeyInfo keyInfo = info.getKeyInfo();
        KeyInfo resourceKeyInfo = info.getResourceKeyInfo();
        SignatureInfo signatureInfo = info.getSignature();
        String content;
        try (InputStream configRs = LauncherCodeGenerator.class.getClassLoader().getResourceAsStream(LAUNCHER_CODE_BUILD_CONFIG_FILE)) {
            content = IOUtils.toString(configRs, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new TransformException("read launcher build config failed", e);
        }
        HashMap<String, String> valueMap = new HashMap<>(4);
        valueMap.put("key", bytesToString(keyInfo.getKey()));
        valueMap.put("resourceKey", bytesToString(resourceKeyInfo.getKey()));
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

        // runtime classes
        File out = new File(launcherClassDir, LAUNCHER_RUNTIME_CLASS_FILE);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                OutputStream fileOutput = Files.newOutputStream(out.toPath())) {
            for (Class<?> clazz : WRITE_RUNTIME_CLASS) {
                // todo 改为运行时编译
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
            ByteArrayInputStream stream = new ByteArrayInputStream(outputStream.toByteArray());
            IOUtils.copy(stream, fileOutput);
        } catch (Exception e) {
            throw new TransformException("write runtime class failed", e);
        }

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
