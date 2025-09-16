package io.kyle.javaguard.constant;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/9/30 13:41
 */
public interface ConstVars {
    String SALT = "kyle java guard salt";
    String META_INF_MANIFEST = "META-INF/MANIFEST.MF";
    String DEFAULT_PRIVATE_KEY = "~/java_guard/key";
    String DEFAULT_PUBLIC_KEY = "~/java_guard/key.pub";
    String ALGORITHM = "AES";
    @SuppressWarnings("unused")
    String TRANSFORMATION = "AES/GCM/NoPadding";
    @SuppressWarnings("unused")
    String SIGN_ALGORITHM = "Ed25519";
    int TRUNK_SIZE = 8192;
    int NONCE_LEN = 12;
    int TAG_LEN = 16;
    int TRANSFORM_BLOCK = TRUNK_SIZE + NONCE_LEN + TAG_LEN;
    /**
     * \0JGR\0
     */
    byte[] ENCRYPT_RESOURCE_HEADER = {0, 74, 71, 82, 0};

    @SuppressWarnings("unused")
    short ZERO_SHORT = 0;

    int ATTRIBUTE_DATA_OFFSET = 6;

    String STATIC_BLOCK_METHOD_NAME = "<clinit>";
    String CONSTRUCTOR_METHOD_NAME = "<init>";

    String TEMP_PATH_PREFIX = "javaguard-data";
}
