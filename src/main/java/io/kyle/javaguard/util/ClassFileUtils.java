package io.kyle.javaguard.util;

import io.kyle.javaguard.constant.ConstVars;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2025/9/15 16:40
 */
public class ClassFileUtils {
    public static boolean isEncryptClass(byte[] bytes) {
        int suffixStart = bytes.length - ConstVars.ENCRYPT_CLASS_SUFFIX.length;
        return suffixStart > 0 && (bytes[suffixStart] & ConstVars.BYTE_L_SIGN) == ConstVars.ENCRYPT_CLASS_SUFFIX[0] &&
                BytesUtils.equalsWith(bytes, suffixStart + 1, ConstVars.ENCRYPT_CLASS_SUFFIX, 1, ConstVars.ENCRYPT_CLASS_SUFFIX.length - 1);
    }
}