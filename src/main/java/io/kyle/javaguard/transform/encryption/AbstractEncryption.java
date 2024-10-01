package io.kyle.javaguard.transform.encryption;

import io.kyle.javaguard.bean.EncryptInfo;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public abstract class AbstractEncryption implements Encryption {
    protected final EncryptInfo encryptInfo;

    public AbstractEncryption(EncryptInfo encryptInfo) {
        this.encryptInfo = encryptInfo;
    }

    protected Cipher getCipher(int opmode) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec sks = new SecretKeySpec(encryptInfo.getKey(), encryptInfo.getAlgorithm());
        Cipher cipher = Cipher.getInstance(encryptInfo.getTransformation());
        cipher.init(opmode, sks);
        return cipher;
    }

    protected byte[] encrypt(byte[] plainText) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return getCipher(Cipher.ENCRYPT_MODE)
                .doFinal(plainText);
    }

    protected byte[] decrypt(byte[] plainText) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return getCipher(Cipher.DECRYPT_MODE)
                .doFinal(plainText);
    }

    protected InputStream encryptStream(InputStream in) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException {
        return new CipherInputStream(in, getCipher(Cipher.ENCRYPT_MODE));
    }

    protected InputStream decryptStream(InputStream in) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException {
        return new CipherInputStream(in, getCipher(Cipher.DECRYPT_MODE));
    }
}
