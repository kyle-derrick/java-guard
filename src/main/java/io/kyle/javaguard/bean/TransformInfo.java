package io.kyle.javaguard.bean;

import java.util.zip.Deflater;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/9/30 10:46
 */
public class TransformInfo {
    private String[] matches;
    private int level = Deflater.DEFAULT_COMPRESSION;
    private EncryptInfo encrypt;
    private SignatureInfo signature;

    public SignatureInfo getSignature() {
        return signature;
    }

    public void setSignature(SignatureInfo signature) {
        this.signature = signature;
    }

    public EncryptInfo getEncrypt() {
        return encrypt;
    }

    public void setEncrypt(EncryptInfo encrypt) {
        this.encrypt = encrypt;
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
}
