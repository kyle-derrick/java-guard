package io.kyle.javaguard.util;

import io.kyle.javaguard.exception.TransformException;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Signer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/29 14:09
 */
public class ZipSignUtils {
    private static final int COMMENT_MAX_LEN = (1 << Short.SIZE) - 1;
    private static final int SUFFIX_ENCODE_LEN = Short.BYTES << 1;
    private static final int READ_BUFFER_SIZE = 4096;
    public static byte[] sign(File zip, Signer signer) throws TransformException {
        try (ZipFile zipFile = new ZipFile(zip);
             RandomAccessFile accessFile = new RandomAccessFile(zip, "rw");) {
            String comment = zipFile.getComment();
            Charset charset = zipFile.getCharset();
            byte[] bytes = comment.getBytes(charset);
            long zipLength = zip.length();
            long commentStart = zipLength - bytes.length - Short.BYTES;

            byte[] buf = new byte[READ_BUFFER_SIZE];
            while (accessFile.getFilePointer() < commentStart) {
                int read;
                if (accessFile.getFilePointer() + READ_BUFFER_SIZE > commentStart) {
                    read = accessFile.read(buf, 0, (int) (commentStart - accessFile.getFilePointer()));
                } else {
                    read = accessFile.read(buf);
                }
                if (read == -1) {
//                    throw new TransformException("signer zip data content failed");
                    throw new TransformException("signer zip data content failed, current read len: " +
                            accessFile.getFilePointer() + "; but comment index: " + commentStart + '/' + zipLength);
                }
                signer.update(buf, 0, read);
            }

            byte[] commentLenBs = new byte[2];
            int read = accessFile.read(commentLenBs);
            if (read <= 0) {
                throw new TransformException("comment length does not found");
            }
            int commentLen = Short.toUnsignedInt(BytesUtils.bytesLeToShort(commentLenBs));
            if (commentLen != bytes.length) {
                throw new TransformException("comment length does not match");
            }
            byte[] sign = signer.generateSignature();
            byte[] hashBase64 = Base64.encodeBase64URLSafe(sign);
            int suffixLen = hashBase64.length + SUFFIX_ENCODE_LEN;
            if (COMMENT_MAX_LEN < suffixLen + commentLen) {
                commentLen = COMMENT_MAX_LEN - suffixLen;
            }
            accessFile.seek(commentStart);
            accessFile.write(BytesUtils.shortToLeBytes((short) (commentLen + suffixLen)));
            if (commentLen > 0) {
                accessFile.write(bytes, 0, commentLen);
            }
            accessFile.write(hashBase64);
            accessFile.write(Hex.encodeHexString(BytesUtils.shortToLeBytes((short) hashBase64.length)).getBytes(charset));
            return sign;
        } catch (IOException e) {
            throw new TransformException("read zip file failed", e);
        } catch (CryptoException e) {
            throw new TransformException("signer option failed", e);
        } catch (RuntimeException e) {
            throw new TransformException("signer zip failed", e);
        }
    }

    public static boolean verify(File zip, Signer signer) throws TransformException {
        try (ZipFile zipFile = new ZipFile(zip);
             FileInputStream inputStream = new FileInputStream(zip);) {
            Charset charset = zipFile.getCharset();
            String comment = zipFile.getComment();
            byte[] commentBytes;
            if (comment == null || (commentBytes = comment.getBytes(charset)).length <= SUFFIX_ENCODE_LEN) {
                throw new TransformException("not found signer in zip file");
            }
            String signLenHex = StringUtils.substring(comment, comment.length() - SUFFIX_ENCODE_LEN, comment.length());
            byte[] sign;
            try {
                int signLen = Short.toUnsignedInt(BytesUtils.bytesLeToShort(Hex.decodeHex(signLenHex)));
                byte[] signBase64 = BytesUtils.subBytes(commentBytes, commentBytes.length - SUFFIX_ENCODE_LEN - signLen, signLen);
                sign = Base64.decodeBase64(signBase64);
            } catch (Exception e) {
                throw new TransformException("can not get zip signer", e);
            }
            long zipLength = zip.length();
            long commentStart = zipLength - commentBytes.length - Short.BYTES;
            byte[] buf = new byte[READ_BUFFER_SIZE];
            int read;
            for (int readLen = 0; readLen < commentStart; ) {
                if ((read = inputStream.read(buf)) == -1) {
                    throw new TransformException("verify zip data content failed, current read len: " +
                            readLen + "; but comment index: " + commentStart + '/' + zipLength);
                }
                readLen += read;
                signer.update(buf, 0, read);
            }
            return signer.verifySignature(sign);
        } catch (IOException e) {
            throw new TransformException("read zip file failed", e);
        } catch (RuntimeException e) {
            throw new TransformException("verify zip failed", e);
        }
    }
}
