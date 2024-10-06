package io.kyle.javaguard.bean;

public class EncryptInfo {
    private byte[] key;
    private String algorithm = "AES";
    private String transformation = "AES/ECB/PKCS5Padding";

    public EncryptInfo() {
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
