package io.kyle.javaguard.support;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.CRC32;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2025/9/16 11:13
 */
public class TransformDataOutputStream extends FilterOutputStream {
    private final CRC32 crc = new CRC32();
    private long size = 0;
    /**
     * Creates an output stream filter built on top of the specified
     * underlying output stream.
     *
     * @param out the underlying output stream to be assigned to
     *            the field {@code this.out} for later use, or
     *            {@code null} if this instance is to be
     *            created without an underlying stream.
     */
    public TransformDataOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void write(int b) throws IOException {
        crc.update(b);
        size++;
        out.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        crc.update(b, off, len);
        size+=len;
        out.write(b, off, len);
    }

    public long getSize() {
        return size;
    }

    public long getCrc() {
        return crc.getValue();
    }
}
