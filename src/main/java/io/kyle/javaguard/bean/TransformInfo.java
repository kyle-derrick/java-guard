package io.kyle.javaguard.bean;

import java.util.zip.Deflater;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/9/30 10:46
 */
public class TransformInfo {
    private String[] matches;
    private int level = Deflater.DEFAULT_COMPRESSION;
    private KeyInfo keyInfo;
    private KeyInfo resourceKeyInfo;
    private SignatureInfo signature;

    public SignatureInfo getSignature() {
        return signature;
    }

    public void setSignature(SignatureInfo signature) {
        this.signature = signature;
    }

    public String[] getMatches() {
        return matches;
    }

    public void setMatches(String[] matches) {
        this.matches = matches;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public KeyInfo getKeyInfo() {
        return keyInfo;
    }

    public void setKeyInfo(KeyInfo keyInfo) {
        this.keyInfo = keyInfo;
    }

    public KeyInfo getResourceKeyInfo() {
        return resourceKeyInfo;
    }

    public void setResourceKeyInfo(KeyInfo resourceKeyInfo) {
        this.resourceKeyInfo = resourceKeyInfo;
    }
}
