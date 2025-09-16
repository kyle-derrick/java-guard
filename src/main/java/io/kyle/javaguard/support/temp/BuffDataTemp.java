package io.kyle.javaguard.support.temp;

import io.kyle.javaguard.support.TransformDataOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2025/9/16 12:46
 */
public class BuffDataTemp implements DataTemp {
    private final ByteArrayOutputStream buff;
    private final TransformDataOutputStream out;

    public BuffDataTemp(int bufferSize) {
        this.buff = new ByteArrayOutputStream(bufferSize);
        this.out = new TransformDataOutputStream(this.buff);
    }

    @Override
    public TransformDataOutputStream getOutputStream() {
        return out;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(buff.toByteArray());
    }
}
