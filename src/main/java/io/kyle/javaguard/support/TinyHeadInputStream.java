package io.kyle.javaguard.support;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2025/5/20 11:32
 */
public class TinyHeadInputStream extends FilterInputStream {
    private static final byte[] ENCRYPT_RESOURCE_HEADER = {0, 74, 71, 82, 0};
    private final byte[] header = new byte[ENCRYPT_RESOURCE_HEADER.length];
    private int curr = 0;
    private final int end;
    private final boolean jgResource;
    /**
     * Creates a <code>FilterInputStream</code>
     * by assigning the  argument <code>in</code>
     * to the field <code>this.in</code> so as
     * to remember it for later use.
     *
     * @param in the underlying input stream, or <code>null</code> if
     *           this instance is to be created without an underlying stream.
     */
    TinyHeadInputStream(InputStream in) throws IOException {
        super(in);
        int len = in.read(header);
        this.end = len;
        this.jgResource = len == ENCRYPT_RESOURCE_HEADER.length && Arrays.equals(ENCRYPT_RESOURCE_HEADER, header);
    }

    private boolean headerOver() {
        return curr == end;
    }

    @Override
    public int read() throws IOException {
        if (headerOver()) {
            return super.read();
        } else {
            return header[curr++] & 0xFF;
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (headerOver()) {
            return super.read(b, off, len);
        } else {
            int copyLen = Math.min(end - curr, len);
            System.arraycopy(header, curr, b, off, copyLen);
            curr += copyLen;
            if (len > copyLen) {
                int readLen = super.read(b, off + copyLen, len - copyLen);
                if (readLen != -1) {
                    return copyLen + readLen;
                }
            }
            return copyLen;
        }
    }

    @Override
    public long skip(long n) throws IOException {
        if (headerOver()) {
            return super.skip(n);
        } else {
            int headerRemaining = end - curr;
            if (headerRemaining >= n) {
                curr += (int) n;
                return n;
            }
            curr = end;
            return headerRemaining + super.skip(n - headerRemaining);
        }
    }

    @Override
    public int available() throws IOException {
        if (headerOver()) {
            return super.available();
        } else {
            return end - curr + super.available();
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
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

    public boolean isJgResource() {
        return jgResource;
    }
}
