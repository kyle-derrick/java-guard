package javassist.bytecode;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/8 13:23
 */
public class CustomCipherInputStream extends FilterInputStream {
    private static final int BUFFER_SIZE = 2048;
    private InputStream input;
    private boolean first = true;

    public CustomCipherInputStream(InputStream in, Cipher cipher) {
        this(new BufferedInputStream(in, BUFFER_SIZE), cipher);
    }

    private CustomCipherInputStream(BufferedInputStream in, Cipher cipher) {
        super(new CipherInputStream(in, cipher));
        this.input = in;
        in.mark(BUFFER_SIZE);
    }

    private boolean isFirstAndChange() throws IOException {
        if (first) {
            super.in = input;
            input.reset();
            first = false;
            return true;
        }
        return false;
    }

    @Override
    public int read() throws IOException {
        try {
            return super.read();
        } catch (IOException e) {
            if (isFirstAndChange()) {
                return super.read();
            }
            throw e;
        }
    }

    @Override
    public int read(byte[] bytes) throws IOException {
        try {
            return super.read(bytes);
        } catch (IOException e) {
            if (isFirstAndChange()) {
                return super.read(bytes);
            }
            throw e;
        }
    }

    @Override
    public int read(byte[] bytes, int i, int i1) throws IOException {
        try {
            return super.read(bytes, i, i1);
        } catch (IOException e) {
            if (isFirstAndChange()) {
                return super.read(bytes, i, i1);
            }
            throw e;
        }
    }

    @Override
    public long skip(long l) throws IOException {
        return super.skip(l);
    }

    @Override
    public int available() throws IOException {
        return super.available();
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    @Override
    public synchronized void mark(int i) {
        super.mark(i);
    }

    @Override
    public synchronized void reset() throws IOException {
        super.reset();
    }

    @Override
    public boolean markSupported() {
        return super.markSupported();
    }
}
