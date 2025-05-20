package io.kyle.javaguard.support;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.StringSubstitutor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.StringJoiner;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/8 16:29
 */
@Deprecated
public class URLExtCode {

    public static String format(String originMethod, byte[] encryptHeader, byte[] resourceKey, String algorithm, String transformation) throws IOException {
        try (InputStream inputStream = URLExtCode.class.getResourceAsStream("/URL.class.ext.java")) {
            HashMap<String, String> valueMap = new HashMap<>(5);
            valueMap.put("originMethod", originMethod);
            valueMap.put("encryptHeader", bytesToString(encryptHeader));
            valueMap.put("resourceKey", bytesToString(resourceKey));
            valueMap.put("algorithm", algorithm);
            valueMap.put("transformation", transformation);
            String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            return new StringSubstitutor(valueMap)
                    .replace(content);
        }
    }

    private static String bytesToString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return StringUtils.EMPTY;
        }
        StringJoiner joiner = new StringJoiner(",");
        for (byte b : bytes) {
            joiner.add(Byte.toString(b));
        }
        return joiner.toString();
    }
}
