package io.kyle.javaguard.util;

import io.kyle.javaguard.exception.TransformException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Signer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/29 14:09
 */
public class ZipSignUtils {
    private static final int COMMENT_MAX_LEN = (1 << Short.SIZE) - 1;
    private static final int SUFFIX_ENCODE_LEN = Short.BYTES << 1;
    private static final int EOCD_MIN_LEN = 22;
    private static final int EOCD_COMMENT_LENGTH_OFFSET = 20;
    private static final int EOCD_SIGNATURE = 0x06054b50;
    private static final int ED25519_SIGNATURE_LEN = 64;
    private static final byte[] SIGNATURE_MAGIC = "JavaGuard-Signature-v1:".getBytes(StandardCharsets.US_ASCII);
    private static final int READ_BUFFER_SIZE = 4096;

    public static byte[] sign(File zip, Signer signer) throws TransformException {
        try (RandomAccessFile accessFile = new RandomAccessFile(zip, "rw")) {
            ZipComment zipComment = readZipComment(accessFile);
            byte[] originalComment = removeSignatureSuffix(zipComment.comment);

            updateSigner(accessFile, zipComment.signingBoundary, signer);
            byte[] signature = signer.generateSignature();
            byte[] encodedSignature = Base64.encodeBase64URLSafe(signature);
            int newCommentLength = originalComment.length + SIGNATURE_MAGIC.length
                    + encodedSignature.length + SUFFIX_ENCODE_LEN;
            if (newCommentLength > COMMENT_MAX_LEN) {
                throw new TransformException("zip comment is too long to append signature");
            }

            accessFile.seek(zipComment.signingBoundary);
            accessFile.write(BytesUtils.shortToLeBytes((short) newCommentLength));
            accessFile.write(originalComment);
            accessFile.write(SIGNATURE_MAGIC);
            accessFile.write(encodedSignature);
            accessFile.write(Hex.encodeHexString(
                    BytesUtils.shortToLeBytes((short) encodedSignature.length)).getBytes(StandardCharsets.US_ASCII));
            accessFile.setLength(zipComment.signingBoundary + Short.BYTES + newCommentLength);
            return signature;
        } catch (CryptoException e) {
            throw new TransformException("signer operation failed", e);
        } catch (IOException e) {
            throw new TransformException("read zip file failed", e);
        } catch (RuntimeException e) {
            throw new TransformException("signer zip failed", e);
        }
    }

    public static boolean verify(File zip, Signer signer) throws TransformException {
        try (RandomAccessFile accessFile = new RandomAccessFile(zip, "r")) {
            ZipComment zipComment = readZipComment(accessFile);
            SignatureSuffix suffix = parseSignatureSuffix(zipComment.comment, true);
            if (suffix == null) {
                throw new TransformException("not found signer in zip file");
            }
            updateSigner(accessFile, zipComment.signingBoundary, signer);
            return signer.verifySignature(suffix.signature);
        } catch (IOException e) {
            throw new TransformException("read zip file failed", e);
        } catch (RuntimeException e) {
            throw new TransformException("verify zip failed", e);
        }
    }

    private static void updateSigner(RandomAccessFile file, long boundary, Signer signer) throws IOException {
        file.seek(0);
        byte[] buffer = new byte[READ_BUFFER_SIZE];
        long remaining = boundary;
        while (remaining > 0) {
            int read = file.read(buffer, 0, (int) Math.min(buffer.length, remaining));
            if (read < 0) {
                throw new IOException("zip ended before signing boundary");
            }
            signer.update(buffer, 0, read);
            remaining -= read;
        }
    }

    private static byte[] removeSignatureSuffix(byte[] comment) {
        SignatureSuffix suffix = parseSignatureSuffix(comment, false);
        return suffix == null ? comment : Arrays.copyOf(comment, suffix.start);
    }

    private static SignatureSuffix parseSignatureSuffix(byte[] comment, boolean allowLegacy) {
        if (comment.length <= SUFFIX_ENCODE_LEN) {
            return null;
        }
        try {
            byte[] trailer = Arrays.copyOfRange(comment, comment.length - SUFFIX_ENCODE_LEN, comment.length);
            for (byte value : trailer) {
                if (!isLowerHex(value)) {
                    return null;
                }
            }
            int encodedLength = Short.toUnsignedInt(BytesUtils.bytesLeToShort(
                    Hex.decodeHex(new String(trailer, StandardCharsets.US_ASCII))));
            int encodedStart = comment.length - SUFFIX_ENCODE_LEN - encodedLength;
            if (encodedLength <= 0 || encodedStart < 0) {
                return null;
            }
            byte[] encoded = Arrays.copyOfRange(comment, encodedStart, comment.length - SUFFIX_ENCODE_LEN);
            if (!isUrlSafeBase64(encoded)) {
                return null;
            }
            byte[] signature = Base64.decodeBase64(encoded);
            if (signature.length != ED25519_SIGNATURE_LEN
                    || !Arrays.equals(encoded, Base64.encodeBase64URLSafe(signature))) {
                return null;
            }
            int markedStart = encodedStart - SIGNATURE_MAGIC.length;
            if (markedStart >= 0 && matchesAt(comment, SIGNATURE_MAGIC, markedStart)) {
                return new SignatureSuffix(markedStart, signature);
            }
            return allowLegacy ? new SignatureSuffix(encodedStart, signature) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean matchesAt(byte[] value, byte[] expected, int offset) {
        for (int i = 0; i < expected.length; i++) {
            if (value[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isLowerHex(byte value) {
        return value >= '0' && value <= '9' || value >= 'a' && value <= 'f';
    }

    private static boolean isUrlSafeBase64(byte[] encoded) {
        for (byte value : encoded) {
            if (!(value >= 'A' && value <= 'Z')
                    && !(value >= 'a' && value <= 'z')
                    && !(value >= '0' && value <= '9')
                    && value != '-' && value != '_') {
                return false;
            }
        }
        return true;
    }

    private static ZipComment readZipComment(RandomAccessFile file) throws IOException, TransformException {
        long fileLength = file.length();
        if (fileLength < EOCD_MIN_LEN) {
            throw new TransformException("invalid zip: end of central directory not found");
        }
        int searchLength = (int) Math.min(fileLength, EOCD_MIN_LEN + COMMENT_MAX_LEN);
        byte[] tail = new byte[searchLength];
        file.seek(fileLength - searchLength);
        file.readFully(tail);
        for (int i = tail.length - EOCD_MIN_LEN; i >= 0; i--) {
            if (readIntLe(tail, i) != EOCD_SIGNATURE) {
                continue;
            }
            int commentLength = readUnsignedShortLe(tail, i + EOCD_COMMENT_LENGTH_OFFSET);
            if (i + EOCD_MIN_LEN + commentLength != tail.length) {
                continue;
            }
            byte[] comment = Arrays.copyOfRange(tail, i + EOCD_MIN_LEN, tail.length);
            long signingBoundary = fileLength - commentLength - Short.BYTES;
            return new ZipComment(signingBoundary, comment);
        }
        throw new TransformException("invalid zip: end of central directory not found");
    }

    private static int readIntLe(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff)
                | (bytes[offset + 1] & 0xff) << 8
                | (bytes[offset + 2] & 0xff) << 16
                | (bytes[offset + 3] & 0xff) << 24;
    }

    private static int readUnsignedShortLe(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff) | (bytes[offset + 1] & 0xff) << 8;
    }

    private static final class ZipComment {
        private final long signingBoundary;
        private final byte[] comment;

        private ZipComment(long signingBoundary, byte[] comment) {
            this.signingBoundary = signingBoundary;
            this.comment = comment;
        }
    }

    private static final class SignatureSuffix {
        private final int start;
        private final byte[] signature;

        private SignatureSuffix(int start, byte[] signature) {
            this.start = start;
            this.signature = signature;
        }
    }
}
