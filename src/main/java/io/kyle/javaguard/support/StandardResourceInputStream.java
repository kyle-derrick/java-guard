package io.kyle.javaguard.support;

import io.kyle.javaguard.bean.KeyInfo;
import io.kyle.javaguard.constant.ConstVars;
import io.kyle.javaguard.exception.TransformException;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2025/5/16 10:08
 */
@SuppressWarnings("DuplicatedCode")
public class StandardResourceInputStream extends FilterInputStream {
    private final KeyInfo keyInfo;
    private final boolean forEncryption;
    private final int bufferSize;
    private final byte[] buffer = new byte[ConstVars.TRANSFORM_BLOCK];
    private int curr = 0;
    private int end = 0;

    public StandardResourceInputStream(InputStream in, KeyInfo keyInfo, boolean forEncryption) {
        super(in);
        this.keyInfo = keyInfo;
        this.forEncryption = forEncryption;
        if (forEncryption) {
            bufferSize = ConstVars.TRUNK_SIZE;
        } else {
            bufferSize = ConstVars.TRANSFORM_BLOCK;
        }
    }


    private byte[] transformer(byte[] data, int off, int len) throws IOException {
        try {
            if (forEncryption) {
                return keyInfo.encrypt(data, off, off + len);
            } else {
                return keyInfo.decrypt(data, off, off + len);
            }
        } catch (TransformException e) {
            throw new IOException(e);
        }
    }

    private boolean checkEofAndLoadNextChunk() throws IOException {
        if (curr == end) {
            loadNextChunk();
        }
        return end == -1;
    }

    private void loadNextChunk() throws IOException {
        int totalRead = 0;
        int read;
        int needRead = bufferSize;
        do {
            read = in.read(buffer, totalRead, needRead);
            if (read == -1) {
                break;
            }
            totalRead += read;
            needRead -= read;
        } while (needRead > 0);

        if (totalRead > 0) {
            byte[] bytes = transformer(buffer, 0, totalRead);
            System.arraycopy(bytes, 0, buffer, 0, bytes.length);
            totalRead = bytes.length;
        } else if (read == -1) {
            totalRead = -1;
        }
        end = totalRead;
        curr = 0;
    }

    @Override
    public int read() throws IOException {
        if (checkEofAndLoadNextChunk()) {
            return -1;
        }
        return buffer[curr++] & 0xFF;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (checkEofAndLoadNextChunk()) {
            return -1;
        }
        int total = 0;
        do {
            int readLen = Math.min(end - curr, len);
            System.arraycopy(buffer, curr, b, off, readLen);
            curr += readLen;
            total += readLen;
            if (len == readLen) {
                break;
            }
            off += readLen;
            len -= readLen;
            loadNextChunk();
        } while (end != -1);
        return total;
    }

    @Override
    public long skip(long n) throws IOException {
        if (checkEofAndLoadNextChunk()) {
            return 0;
        }
        long remaining = n;

        do {
            int bufferRemaining = end - curr;
            if (remaining <= bufferRemaining) {
                curr += bufferRemaining;
                return n;
            }
            remaining -= bufferRemaining;
            curr = end;
            loadNextChunk();
        } while (end != -1);
        return n - remaining;
    }

    @Override
    public int available() throws IOException {
        if (end == -1) {
            return 0;
        }
        return end - curr + super.available();
    }

    @Override
    public synchronized void mark(int readlimit) {
    }

    @Override
    public synchronized void reset() throws IOException {
    }

    @Override
    public boolean markSupported() {
        return false;
    }
}
