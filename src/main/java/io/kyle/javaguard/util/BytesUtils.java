package io.kyle.javaguard.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/11 14:01
 */
public class BytesUtils {

    public static byte[] intToBytes(int i) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(i);
        return buffer.array();
    }

    public static short bytesToShort(byte[] bs) {
        ByteBuffer buffer = ByteBuffer.wrap(bs);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getShort();
    }

    public static byte[] shortToBytes(short s) {
        ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort(s);
        return buffer.array();
    }

    public static byte[] subBytes(byte[] bs, int start, int len) {
        byte[] bytes = new byte[len];
        System.arraycopy(bs, start, bytes, 0, len);
        return bytes;
    }
}
