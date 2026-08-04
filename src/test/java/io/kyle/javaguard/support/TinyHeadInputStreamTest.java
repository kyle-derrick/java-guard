package io.kyle.javaguard.support;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class TinyHeadInputStreamTest {
    private static final byte[] ENCRYPT_RESOURCE_HEADER = {0, 74, 71, 82, 0};

    @Test
    public void recognizesEncryptedHeaderWithShortReads() throws Exception {
        byte[] data = concat(ENCRYPT_RESOURCE_HEADER, new byte[]{1, 2, 3});

        TinyHeadInputStream input = new TinyHeadInputStream(new OneByteAtATimeInputStream(data));

        Assert.assertTrue(input.isJgResource());
        Assert.assertArrayEquals(data, read(input));
    }

    @Test
    public void replaysPlainResourceWithShortReads() throws Exception {
        byte[] data = {1, 2, 3, 4, 5, 6, 7};

        TinyHeadInputStream input = new TinyHeadInputStream(new OneByteAtATimeInputStream(data));

        Assert.assertFalse(input.isJgResource());
        Assert.assertArrayEquals(data, read(input));
    }

    @Test
    public void replaysPlainResourceShorterThanHeader() throws Exception {
        byte[] data = {1, 2, 3};

        TinyHeadInputStream input = new TinyHeadInputStream(new OneByteAtATimeInputStream(data));

        Assert.assertFalse(input.isJgResource());
        Assert.assertArrayEquals(data, read(input));
    }

    private static byte[] read(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[16];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private static final class OneByteAtATimeInputStream extends ByteArrayInputStream {
        private OneByteAtATimeInputStream(byte[] data) {
            super(data);
        }

        @Override
        public synchronized int read(byte[] buffer, int offset, int length) {
            return super.read(buffer, offset, Math.min(length, 1));
        }
    }
}
