package io.kyle.javaguard.transform;

import io.kyle.javaguard.bean.EncryptInfo;
import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.exception.TransformException;
import org.apache.commons.io.IOUtils;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/08 10:32
 */
public abstract class AbstractTransformer implements Transformer {
    protected final TransformInfo transformInfo;
    protected final EncryptInfo encryptInfo;

    public AbstractTransformer(TransformInfo transformInfo) {
        this.transformInfo = transformInfo;
        this.encryptInfo = transformInfo.getEncrypt();
    }

    protected byte[] encrypt(byte[] plainText) throws TransformException {
        return encryptInfo.encrypt(plainText);
    }

    protected byte[] decrypt(byte[] plainText) throws TransformException {
        return encryptInfo.decrypt(plainText);
    }

    protected InputStream encryptStream(InputStream in) throws TransformException {
        return new CipherInputStream(in, encryptInfo.getCipher(Cipher.ENCRYPT_MODE));
    }

    protected OutputStream encryptStream(OutputStream out) throws TransformException {
        return new CipherOutputStream(out, encryptInfo.getCipher(Cipher.ENCRYPT_MODE));
    }

    protected InputStream decryptStream(InputStream in) throws TransformException {
        return new CipherInputStream(in, encryptInfo.getCipher(Cipher.DECRYPT_MODE));
    }

    protected OutputStream decryptStream(OutputStream out) throws TransformException {
        return new CipherOutputStream(out, encryptInfo.getCipher(Cipher.DECRYPT_MODE));
    }

    protected void copyStream(InputStream in, OutputStream out) throws TransformException {
        try {
            IOUtils.copy(in, out);
        } catch (IOException e) {
            throw new TransformException(e);
        }
    }
}
