package io.kyle.javaguard.support;

import io.kyle.javaguard.bean.EncryptInfo;
import io.kyle.javaguard.bean.SignatureInfo;
import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.constant.ConstVars;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.StringJoiner;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/8 22:29
 */
public class LauncherCodeGenerator {
    private static final String LAUNCHER_CODE_DIR = "jg-launcher";
    private static final String LAUNCHER_CODE_BUILD_CONFIG_FILE = "build_config.rs";
    private static final String LAUNCHER_CODE_BUILD_CONFIG_PATH = "build" + File.separatorChar + LAUNCHER_CODE_BUILD_CONFIG_FILE;
    public static void generate(String output, TransformInfo info) throws IOException {
        EncryptInfo encrypt = info.getEncrypt();
        SignatureInfo signatureInfo = info.getSignature();
        String content;
        try (InputStream configRs = LauncherCodeGenerator.class.getClassLoader().getResourceAsStream(LAUNCHER_CODE_BUILD_CONFIG_FILE)) {
            content = IOUtils.toString(configRs, StandardCharsets.UTF_8);
        }
        HashMap<String, String> valueMap = new HashMap<>(4);
        valueMap.put("key", bytesToString(encrypt.getKey()));
        valueMap.put("resourceKey", bytesToString(encrypt.getResourceKey()));
        valueMap.put("publicKey", bytesToString(signatureInfo.getPublicKey()));
        valueMap.put("signKeyVersion", signatureInfo.getKeyHash());
        content = new StringSubstitutor(valueMap).replace(content);
        File launcherDir = new File(output, LAUNCHER_CODE_DIR);
        if (!launcherDir.exists()) {
            launcherDir.mkdirs();
        }
        // todo copy to launcher dir
        FileUtils.write(new File(launcherDir, LAUNCHER_CODE_BUILD_CONFIG_PATH), content, StandardCharsets.UTF_8);
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
