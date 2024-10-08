package io.kyle.javaguard.bean;

import io.kyle.javaguard.constant.ConstVars;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/08 10:32
 */
public class EncryptInfo {
    private byte[] key;
    private byte[] resourceKey;
    private String algorithm = "AES";
    private String transformation = "AES/ECB/PKCS5Padding";

    public EncryptInfo() {
    }

    public byte[] getResourceKey() {
        if (resourceKey == null && key != null) {
            resourceKey = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, ConstVars.SALT).hmac(key);
        }
        return resourceKey;
    }

    public EncryptInfo(byte[] key) {
        this.key = key;
    }

    public byte[] getKey() {
        return key;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getTransformation() {
        return transformation;
    }

    public void setTransformation(String transformation) {
        this.transformation = transformation;
    }
}
