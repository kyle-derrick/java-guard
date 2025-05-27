package io.kyle.javaguard.constant;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/9/30 13:41
 */
public interface ConstVars {
    String SALT = "kyle java guard salt";
    String META_INF_MANIFEST = "META-INF/MANIFEST.MF";
    String DEFAULT_PRIVATE_KEY = "~/java_guard/private_key.pem";
    String DEFAULT_PUBLIC_KEY = "~/java_guard/public_key.pem";
    String ALGORITHM = "AES";
    String TRANSFORMATION = "AES/GCM/NoPadding";
    int TRANSFORM_BLOCK = 8192;
    int NONCE_LEN = 12;
    int TAG_LEN = 16;
    /**
     * \0JGR\0
     */
    byte[] ENCRYPT_RESOURCE_HEADER = {0, 74, 71, 82, 0};
}
