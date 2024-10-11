package io.kyle.javaguard.util;

import java.nio.ByteBuffer;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/11 14:01
 */
public class BytesUtils {

    public static byte[] intToBytes(int i) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(i);
        return buffer.array();
    }
}
