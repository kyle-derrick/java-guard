package io.kyle.javaguard.bean;

import io.kyle.javaguard.constant.TransformType;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/08 10:32
 */
public class AppConfig {
    private String[] matches;
    private Integer zipLevel;
    private String key;
    private String privateKey;
    private String publicKey;
    private TransformType mode = TransformType.encrypt;
    private String output = "./out";
    private long bufferSize = 1024 * 1024;
    private boolean printEncryptEntry = true;

    public String[] getMatches() {
        return matches;
    }

    public void setMatches(String[] matches) {
        this.matches = matches;
    }

    public Integer getZipLevel() {
        return zipLevel;
    }

    public void setZipLevel(Integer zipLevel) {
        this.zipLevel = zipLevel;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public TransformType getMode() {
        return mode;
    }

    public void setMode(TransformType mode) {
        this.mode = mode;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public long getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(long bufferSize) {
        this.bufferSize = bufferSize;
    }

    public boolean isPrintEncryptEntry() {
        return printEncryptEntry;
    }

    public void setPrintEncryptEntry(boolean printEncryptEntry) {
        this.printEncryptEntry = printEncryptEntry;
    }
}
