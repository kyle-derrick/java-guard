package io.kyle.javaguard.support;

import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.modes.GCMModeCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2025/5/16 10:08
 */
public class AesGcmResourceOutputStream extends FilterOutputStream {
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final int AES_GCM_TAG_SIZE = 16;

    private final byte[] initIv;
    private AEADParameters parameters;
    private final int bufferSize;
    private final GCMModeCipher cipher;
    private final boolean forEncryption;
    private final byte[] buffer;
    private final byte[] handledBuffer;
    private int curr = 0;

    /**
     * Creates an output stream filter built on top of the specified
     * underlying output stream.
     *
     * @param out the underlying output stream to be assigned to
     *            the field <tt>this.out</tt> for later use, or
     *            <code>null</code> if this instance is to be
     *            created without an underlying stream.
     */
    public AesGcmResourceOutputStream(OutputStream out, byte[] key, byte[] iv, boolean forEncryption) {
        this(out, key, iv, forEncryption, DEFAULT_BUFFER_SIZE);
    }
    public AesGcmResourceOutputStream(OutputStream out, byte[] key, byte[] iv, boolean forEncryption, int bufferSize) {
        super(out);
        KeyParameter keyParameter = new KeyParameter(key);
        GCMModeCipher cipher = GCMBlockCipher.newInstance(AESEngine.newInstance());
        AEADParameters parameters = new AEADParameters(keyParameter, 128, iv, ArrayUtils.EMPTY_BYTE_ARRAY);
        cipher.init(forEncryption, parameters);
        this.parameters = parameters;
        this.cipher = cipher;
        this.initIv = iv.clone();
        this.forEncryption = forEncryption;
        int handledBufferSize = bufferSize;
        if (forEncryption) {
            handledBufferSize += AES_GCM_TAG_SIZE;
        } else {
            bufferSize += AES_GCM_TAG_SIZE;
        }
        this.bufferSize = bufferSize;
        this.buffer = new byte[bufferSize];
        this.handledBuffer = new byte[handledBufferSize];
    }

    private void doFinalFlush() throws IOException {
        try {
            int hbLen = cipher.processBytes(buffer, 0, curr, handledBuffer, 0);
            out.write(handledBuffer, 0, hbLen);
            hbLen = cipher.doFinal(handledBuffer, 0);
            if (hbLen > 0) {
                out.write(handledBuffer, 0, hbLen);
            }
            byte[] iv = parameters.getNonce();
            for (int i = iv.length - 1; i >= 0; i--) {
                iv[i] ++;
                if (iv[i] != initIv[i]) {
                    break;
                }
            }
//            cipher.reset();
            AEADParameters parameters = new AEADParameters(this.parameters.getKey(), 128, iv, ArrayUtils.EMPTY_BYTE_ARRAY);
            this.parameters = parameters;
            cipher.init(forEncryption, parameters);
            curr = 0;
        } catch (InvalidCipherTextException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(int b) throws IOException {
        buffer[curr++] = (byte) b;
        if (curr == bufferSize) {
            doFinalFlush();
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        do {
            int firstLen = Math.min(bufferSize - curr, len);
            if (firstLen > 0) {
                System.arraycopy(b, off, buffer, curr, firstLen);
                curr += firstLen;
            }
            if (curr == bufferSize) {
                doFinalFlush();
                off += firstLen;
                len -= firstLen;
            } else {
                return;
            }
        } while (len != 0);
    }

    @Override
    public void close() throws IOException {
        if (curr > 0) {
            doFinalFlush();
        }
        super.close();
    }
}
