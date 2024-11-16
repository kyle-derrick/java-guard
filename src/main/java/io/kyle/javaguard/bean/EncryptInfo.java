package io.kyle.javaguard.bean;

import io.kyle.javaguard.constant.ConstVars;
import io.kyle.javaguard.exception.TransformException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/08 10:32
 */
public class EncryptInfo {
    private byte[] key;
    private byte[] resourceKey;
    private String algorithm = "AES";
    private String transformation = "AES/GCM/NoPadding";

    public EncryptInfo() {
    }

    private Cipher getCipherBy(byte[] key, int opmode) throws TransformException {
        try {
            SecretKeySpec sks = new SecretKeySpec(key, getAlgorithm());
            Cipher cipher = Cipher.getInstance(getTransformation());
            GCMParameterSpec spec = new GCMParameterSpec(128, DigestUtils.md5(key));
            cipher.init(opmode, sks, spec);
            return cipher;
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                 InvalidAlgorithmParameterException e) {
            throw new TransformException("cipher init failed", e);
        }
    }

    public Cipher getCipher(int opmode) throws TransformException {
        return getCipherBy(getKey(), opmode);
    }

    public Cipher getResourceCipher(int opmode) throws TransformException {
        return getCipherBy(getResourceKey(), opmode);
    }

    public byte[] encrypt(byte[] plainText) throws TransformException {
            return cipherDoFinal(getCipher(Cipher.ENCRYPT_MODE), plainText);
    }

    public byte[] decrypt(byte[] plainText) throws TransformException {
        return cipherDoFinal(getCipher(Cipher.DECRYPT_MODE), plainText);
    }

    public byte[] resourceEncrypt(byte[] plainText) throws TransformException {
        return cipherDoFinal(getResourceCipher(Cipher.ENCRYPT_MODE), plainText);
    }

    public byte[] resourceDecrypt(byte[] plainText) throws TransformException {
        return cipherDoFinal(getResourceCipher(Cipher.DECRYPT_MODE), plainText);
    }

    private byte[] cipherDoFinal(Cipher cipher, byte[] plainText) throws TransformException {
        try {
            return cipher.doFinal(plainText);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new TransformException("encrypt/decrypt failed", e);
        }
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
