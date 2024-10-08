package io.kyle.javaguard.transform;

import io.kyle.javaguard.bean.EncryptInfo;
import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.exception.TransformException;
import org.apache.commons.io.IOUtils;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/08 10:32
 */
public abstract class AbstractTransformer implements Transformer {
    protected final TransformInfo transformInfo;

    public AbstractTransformer(TransformInfo transformInfo) {
        this.transformInfo = transformInfo;
    }

    protected Cipher getCipher(int opmode) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        EncryptInfo encryptInfo = transformInfo.getEncrypt();
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

    protected OutputStream encryptStream(OutputStream out) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException {
        return new CipherOutputStream(out, getCipher(Cipher.ENCRYPT_MODE));
    }

    protected InputStream decryptStream(InputStream in) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException {
        return new CipherInputStream(in, getCipher(Cipher.DECRYPT_MODE));
    }

    protected OutputStream decryptStream(OutputStream out) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException {
        return new CipherOutputStream(out, getCipher(Cipher.DECRYPT_MODE));
    }

    protected void copyStream(InputStream in, OutputStream out) throws TransformException {
        try {
            IOUtils.copy(in, out);
        } catch (IOException e) {
            throw new TransformException(e);
        }
    }
}
