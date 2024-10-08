package io.kyle.javaguard.bean;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/08 10:32
 */
public class SignatureInfo {
    private byte[] privateKey;
    private byte[] publicKey;
    private String algorithm = "Ed25519";
    private Signature signature;

    public Signature getSignature() throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException {
        if (signature == null) {
            signature = newSignature();
        }
        return signature;
    }

    public Signature newSignature() throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(getPrivateKey());
        KeyFactory keyFactory = KeyFactory.getInstance(getAlgorithm());
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        Signature signature = Signature.getInstance(getAlgorithm());
        signature.initSign(privateKey);
        return signature;
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(byte[] privateKey) {
        this.privateKey = privateKey;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
}
