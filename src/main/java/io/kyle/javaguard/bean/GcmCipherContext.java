package io.kyle.javaguard.bean;

import io.kyle.javaguard.exception.TransformException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.modes.GCMModeCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

public class GcmCipherContext {
    public static final int IV_SIZE = 12;
    public static final int TAG_BITS = 128;
    public static final int TAG_SIZE = TAG_BITS >> 3;
    private final GCMModeCipher cipher;
    private final boolean forEncryption;
    private final KeyParameter key;
    private final byte[] nonce;

    public GcmCipherContext(boolean forEncryption, KeyParameter key, byte[] nonce) {
        this.cipher = GCMBlockCipher.newInstance(AESEngine.newInstance());
        this.cipher.init(forEncryption, new AEADParameters(key, TAG_BITS, nonce));
        this.forEncryption = forEncryption;
        this.key = key;
        this.nonce = nonce;
    }

    public GCMModeCipher getCipher() {
        return cipher;
    }

    public boolean isForEncryption() {
        return forEncryption;
    }

    public KeyParameter getKey() {
        return key;
    }

    public byte[] getNonce() {
        return nonce.clone();
    }

    public int getOutputSize(int dataLength) {
        if (forEncryption) {
            dataLength += TAG_SIZE;
        } else {
            dataLength -= TAG_SIZE;
        }
        return dataLength;
    }

    public byte[] transform(byte[] data) throws TransformException {
        byte[] output = new byte[getOutputSize(data.length)];
        transform(data, 0, data.length, output, 0);
        return output;
    }

    public int transform(byte[] data, int off, int end, byte[] output, int outOff) throws TransformException {
        if (output.length - outOff < getOutputSize(end - off)) {
            throw new TransformException("output too short");
        }
        try {
            int len = cipher.processBytes(data, off, end - off, output, outOff);
            int outEnd = outOff + len;
            outEnd += cipher.doFinal(output, outOff + len);
            return outEnd;
        } catch (InvalidCipherTextException e) {
            throw new TransformException("encrypt/decrypt failed", e);
        }
    }
}
