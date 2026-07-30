package io.kyle.javaguard.test;

import io.kyle.javaguard.bean.KeyInfo;
import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.constant.ConstVars;
import io.kyle.javaguard.support.StandardResourceInputStream;
import io.kyle.javaguard.transform.DefaultTransformer;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class ResourceStreamTest {
    private static final KeyInfo KEY_INFO = new KeyInfo(new byte[32]);

    @Test
    public void decryptPreservesPlainResourcesShorterThanHeader() throws Exception {
        DefaultTransformer transformer = newTransformer();
        for (int length = 0; length < ConstVars.ENCRYPT_RESOURCE_HEADER.length; length++) {
            byte[] plain = data(length);
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            transformer.decrypt(new ByteArrayInputStream(plain), output);

            Assert.assertArrayEquals("plain resource length " + length, plain, output.toByteArray());
        }
    }

    @Test
    public void encryptHandlesNonMarkSupportingInputAndDetectsExistingHeader() throws Exception {
        DefaultTransformer transformer = newTransformer();
        byte[] plain = data(73);
        ByteArrayOutputStream encrypted = new ByteArrayOutputStream();

        transformer.encrypt(new NonMarkInputStream(plain), encrypted);
        byte[] encryptedOnce = encrypted.toByteArray();
        ByteArrayOutputStream encryptedAgain = new ByteArrayOutputStream();
        transformer.encrypt(new NonMarkInputStream(encryptedOnce), encryptedAgain);

        Assert.assertArrayEquals(encryptedOnce, encryptedAgain.toByteArray());
        ByteArrayOutputStream decrypted = new ByteArrayOutputStream();
        transformer.decrypt(new OneByteAtATimeInputStream(encryptedOnce), decrypted);
        Assert.assertArrayEquals(plain, decrypted.toByteArray());
    }

    @Test
    public void resourceRoundTripHandlesShortReadsAndChunkBoundaries() throws Exception {
        int[] lengths = {0, 1, 4, ConstVars.TRUNK_SIZE, ConstVars.TRUNK_SIZE + 1,
                ConstVars.TRUNK_SIZE * 2 + 37};
        DefaultTransformer transformer = newTransformer();
        for (int length : lengths) {
            byte[] plain = data(length);
            ByteArrayOutputStream encrypted = new ByteArrayOutputStream();
            transformer.encrypt(new OneByteAtATimeInputStream(plain), encrypted);

            ByteArrayOutputStream decrypted = new ByteArrayOutputStream();
            transformer.decrypt(new OneByteAtATimeInputStream(encrypted.toByteArray()), decrypted);

            Assert.assertArrayEquals("round trip length " + length, plain, decrypted.toByteArray());
        }
    }

    @Test
    public void skipAdvancesByRequestedAmountWithinCurrentChunk() throws Exception {
        byte[] plain = data(128);
        InputStream decrypted = encryptedStream(plain);

        Assert.assertEquals(0, decrypted.skip(0));
        Assert.assertEquals(1, decrypted.skip(1));
        Assert.assertEquals(plain[1] & 0xff, decrypted.read());
        Assert.assertEquals(17, decrypted.skip(17));
        Assert.assertEquals(plain[19] & 0xff, decrypted.read());
    }

    @Test
    public void skipHandlesChunkBoundaryAndEndOfStream() throws Exception {
        byte[] plain = data(ConstVars.TRUNK_SIZE * 2 + 19);
        InputStream decrypted = encryptedStream(plain);

        Assert.assertEquals(ConstVars.TRUNK_SIZE, decrypted.skip(ConstVars.TRUNK_SIZE));
        Assert.assertEquals(plain[ConstVars.TRUNK_SIZE] & 0xff, decrypted.read());
        long remaining = plain.length - ConstVars.TRUNK_SIZE - 1L;
        Assert.assertEquals(remaining, decrypted.skip(remaining + 100));
        Assert.assertEquals(-1, decrypted.read());
        Assert.assertEquals(0, decrypted.skip(1));
    }

    @Test
    public void skipAfterPartialReadCanCrossChunks() throws Exception {
        byte[] plain = data(ConstVars.TRUNK_SIZE * 2 + 11);
        InputStream decrypted = encryptedStream(plain);
        byte[] prefix = new byte[31];
        Assert.assertEquals(prefix.length, decrypted.read(prefix));
        Assert.assertArrayEquals(Arrays.copyOf(plain, prefix.length), prefix);

        long skip = ConstVars.TRUNK_SIZE + 7L;
        Assert.assertEquals(skip, decrypted.skip(skip));
        Assert.assertEquals(plain[prefix.length + (int) skip] & 0xff, decrypted.read());
    }

    private static DefaultTransformer newTransformer() {
        TransformInfo transformInfo = new TransformInfo();
        transformInfo.setResourceKeyInfo(KEY_INFO);
        return new DefaultTransformer(transformInfo);
    }

    private static InputStream encryptedStream(byte[] plain) throws Exception {
        ByteArrayOutputStream encrypted = new ByteArrayOutputStream();
        try (InputStream input = new StandardResourceInputStream(
                new ByteArrayInputStream(plain), KEY_INFO, true)) {
            copy(input, encrypted);
        }
        return new StandardResourceInputStream(
                new ByteArrayInputStream(encrypted.toByteArray()), KEY_INFO, false);
    }

    private static byte[] data(int length) {
        byte[] data = new byte[length];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i * 31 + 7);
        }
        return data;
    }

    private static void copy(InputStream input, ByteArrayOutputStream output) throws IOException {
        byte[] buffer = new byte[257];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
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

    private static final class NonMarkInputStream extends InputStream {
        private final ByteArrayInputStream delegate;

        private NonMarkInputStream(byte[] data) {
            delegate = new ByteArrayInputStream(data);
        }

        @Override
        public int read() {
            return delegate.read();
        }

        @Override
        public int read(byte[] buffer, int offset, int length) {
            return delegate.read(buffer, offset, Math.min(length, 2));
        }

        @Override
        public boolean markSupported() {
            return false;
        }
    }
}
