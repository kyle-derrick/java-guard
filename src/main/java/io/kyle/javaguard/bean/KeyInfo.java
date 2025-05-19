package io.kyle.javaguard.bean;

import io.kyle.javaguard.exception.TransformException;
import org.bouncycastle.crypto.params.KeyParameter;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/08 10:32
 */
public class KeyInfo {
    private final byte[] key;
    private final SecureRandom secureRandom = new SecureRandom();

    public KeyInfo(byte[] key) {
        this.key = key;
    }

    public byte[] getKey() {
        return key.clone();
    }

    public KeyParameter getKeyParams() {
        return new KeyParameter(getKey());
    }

    public GcmCipherContext getCipher(boolean forEncryption) throws TransformException {
        return new GcmCipherContext(forEncryption, getKeyParams(), nextIv());
    }

    public GcmCipherContext getCipher(boolean forEncryption, byte[] iv) throws TransformException {
        return new GcmCipherContext(forEncryption, getKeyParams(), iv);
    }

    public byte[] nextIv() {
        byte[] iv = new byte[12];
        secureRandom.nextBytes(iv);
        return iv;
    }

    public byte[] encrypt(byte[] plainText) throws TransformException {
        return encrypt(plainText, 0, plainText.length);
    }

    public byte[] encrypt(byte[] plainText, int off, int end) throws TransformException {
        GcmCipherContext context = this.getCipher(true);
        byte[] nonce = context.getNonce();
        byte[] output = new byte[context.getOutputSize(end - off) + GcmCipherContext.IV_SIZE];
        context.transform(plainText, off, end, output, GcmCipherContext.IV_SIZE);
        System.arraycopy(nonce, off, output, 0, GcmCipherContext.IV_SIZE);
        return output;
    }

    public byte[] decrypt(byte[] plainText) throws TransformException {
        return decrypt(plainText, 0, plainText.length);
    }

    public byte[] decrypt(byte[] plainText, int off, int end) throws TransformException {
        byte[] iv = Arrays.copyOfRange(plainText, off, off += GcmCipherContext.IV_SIZE);
        GcmCipherContext context = this.getCipher(false, iv);
        byte[] output = new byte[context.getOutputSize(end - off)];
        context.transform(plainText, off, end, output, 0);
        return output;
    }
}
