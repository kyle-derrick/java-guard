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
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(i);
        return buffer.array();
    }

    public static short bytesLeToShort(byte[] bs) {
        ByteBuffer buffer = ByteBuffer.wrap(bs);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getShort();
    }

    public static int bytesToInt(byte[] bs, int offset) {
        ByteBuffer buffer = ByteBuffer.wrap(bs, offset, Integer.BYTES);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer.getInt();
    }

    public static byte[] shortToBytes(short s, ByteOrder order) {
        ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES);
        buffer.order(order);
        buffer.putShort(s);
        return buffer.array();
    }

    public static byte[] shortToLeBytes(short s) {
        return shortToBytes(s, ByteOrder.LITTLE_ENDIAN);
    }

    public static byte[] subBytes(byte[] bs, int start, int len) {
        byte[] bytes = new byte[len];
        System.arraycopy(bs, start, bytes, 0, len);
        return bytes;
    }

    public static boolean equalsWith(byte[] a, int offsetA, byte[] b, int offsetB, int len) {
        for (int i = 0; i < len; i++) {
            if (a[offsetA + i] != b[offsetB + i]) {
                return false;
            }
        }
        return true;
    }
}
