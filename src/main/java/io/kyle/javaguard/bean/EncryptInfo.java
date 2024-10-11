package io.kyle.javaguard.bean;

import io.kyle.javaguard.constant.ConstVars;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
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
    private String transformation = "AES/ECB/PKCS5Padding";

    public EncryptInfo() {
    }

    public Cipher getCipher(int opmode) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec sks = new SecretKeySpec(getKey(), getAlgorithm());
        Cipher cipher = Cipher.getInstance(getTransformation());
        cipher.init(opmode, sks);
        return cipher;
    }

    public Cipher getResourceCipher(int opmode) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec sks = new SecretKeySpec(getKey(), getAlgorithm());
        Cipher cipher = Cipher.getInstance(getTransformation());
        cipher.init(opmode, sks);
        return cipher;
    }

    public byte[] encrypt(byte[] plainText) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return getCipher(Cipher.ENCRYPT_MODE)
                .doFinal(plainText);
    }

    public byte[] decrypt(byte[] plainText) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return getCipher(Cipher.DECRYPT_MODE)
                .doFinal(plainText);
    }

    public byte[] resourceEncrypt(byte[] plainText) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return getCipher(Cipher.ENCRYPT_MODE)
                .doFinal(plainText);
    }

    public byte[] resourceDecrypt(byte[] plainText) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return getCipher(Cipher.DECRYPT_MODE)
                .doFinal(plainText);
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
