package io.kyle.javaguard.bean;

import io.kyle.javaguard.constant.ConstVars;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/08 10:32
 */
public class SignatureInfo {
    private byte[] privateKey;
    private byte[] publicKey;
    private String algorithm = "Ed25519";
    private Signature signature;

    public Signature getSignSignature() throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException {
        if (signature == null) {
            signature = newSignSignature();
        }
        return signature;
    }

    public String getKeyHash() {
        if (publicKey == null) {
            return "-";
        }
        return new HmacUtils(HmacAlgorithms.HMAC_MD5, ConstVars.SALT)
                .hmacHex(publicKey);
    }

    public Signature newSignSignature() throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(getPrivateKey());
        KeyFactory keyFactory = KeyFactory.getInstance(getAlgorithm());
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        Signature signature = Signature.getInstance(getAlgorithm());
        signature.initSign(privateKey);
        return signature;
    }

    public Signature newVerifySignature() throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(getPublicKey());
        KeyFactory keyFactory = KeyFactory.getInstance(getAlgorithm());
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        Signature signature = Signature.getInstance(getAlgorithm());
        signature.initVerify(publicKey);
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
