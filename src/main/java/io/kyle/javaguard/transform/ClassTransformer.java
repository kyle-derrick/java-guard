package io.kyle.javaguard.transform;

import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.constant.ConstVars;
import io.kyle.javaguard.exception.TransformException;
import io.kyle.javaguard.support.asm.JGInfoAttribute;
import io.kyle.javaguard.util.BytesUtils;
import io.kyle.javaguard.util.ClassFileUtils;
import io.kyle.javaguard.util.ClassStubGenerator;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/9/30 14:17
 */
public class ClassTransformer extends AbstractTransformer {
    private final LZ4Factory lz4Factory = LZ4Factory.fastestJavaInstance();
    private final LZ4Compressor lz4Compressor = lz4Factory.highCompressor();
    private final LZ4FastDecompressor lz4Decompressor = lz4Factory.fastDecompressor();
    private byte[] buffer = ArrayUtils.EMPTY_BYTE_ARRAY;

    public ClassTransformer(TransformInfo transformInfo) {
        super(transformInfo);
    }

    @Override
    public boolean isSupport(String name) {
        return name.endsWith(".class");
    }

    @Override
    public boolean encrypt(InputStream in, OutputStream out) throws TransformException {
        byte[] oriBytes;
        try {
            oriBytes = IOUtils.toByteArray(in);
            if (ClassFileUtils.isEncryptClass(oriBytes)) {
                out.write(oriBytes);
                return true;
            }
        } catch (IOException e) {
            throw new TransformException("analysis class byte failed", e);
        }
        int maxLen = lz4Compressor.maxCompressedLength(oriBytes.length) + 4;
        if (maxLen > buffer.length) {
            buffer = new byte[maxLen];
        }
        ByteBuffer buffer = ByteBuffer.wrap(this.buffer);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(oriBytes.length);
        int len = lz4Compressor.compress(oriBytes, 0, oriBytes.length, this.buffer, Integer.BYTES);
        byte[] encrypt = transformInfo.getKeyInfo().encrypt(this.buffer, 0, len + Integer.BYTES);
        byte[] bytes = ClassStubGenerator.generateStubClass(oriBytes, node -> {
            if (node.attrs == null) {
                node.attrs = new ArrayList<>();
            }
            node.attrs.add(new JGInfoAttribute(encrypt, 0, encrypt.length));
        });
        try {
            out.write(bytes);
        } catch (IOException e) {
            throw new TransformException("write class byte failed", e);
        }
        return true;
    }

    @Override
    public boolean decrypt(InputStream in, OutputStream out) throws TransformException {
        byte[] bytes;
        try {
            bytes = IOUtils.toByteArray(in);
            if (ClassFileUtils.isEncryptClass(bytes)) {
                int hasSignIndex = bytes.length - ConstVars.ENCRYPT_CLASS_SUFFIX.length;
                boolean hasSign = bytes[hasSignIndex] != ConstVars.ENCRYPT_CLASS_SUFFIX[0];
                int lenIndex = hasSignIndex - Integer.BYTES;
                if (hasSign) {
                    lenIndex -= ConstVars.SIGN_LENGTH;
                }
                int dataLen = BytesUtils.bytesToInt(bytes, lenIndex);
                int dataStart = lenIndex - dataLen;// encrypt 2555 compress 2523 ori 4473
                bytes = transformInfo.getKeyInfo().decrypt(bytes, dataStart, lenIndex);

                int decompressLen = BytesUtils.bytesToInt(bytes, 0);
                if (decompressLen > buffer.length) {
                    buffer = new byte[decompressLen];
                }
                lz4Decompressor.decompress(bytes, Integer.BYTES, buffer, 0, decompressLen);
                out.write(buffer, 0, decompressLen);
            } else {
                out.write(bytes);
            }
        } catch (IOException e) {
            throw new TransformException("decrypt class data failed", e);
        }
        return true;
    }
}
