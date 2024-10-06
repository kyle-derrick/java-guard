package javassist.bytecode;

import io.kyle.javaguard.bean.ClassTransformInfo;
import io.kyle.javaguard.constant.ClassAttribute;
import io.kyle.javaguard.constant.ConstBaseType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

public class ClassTransformUtils {
    private static final Set<String> RETAIN_STRING = new HashSet<>();
    private static final int CONST_INFO_PREFIX_SIZE = 3;
    private static final byte[] CONST_INFO_INDEX_PREFIX = {0,0};
    private static final int CONST_INFO_SIZE_ESTIMATE = 2 + 1 + 10;

    static {
        for (ClassAttribute value : ClassAttribute.values()) {
            RETAIN_STRING.add(value.name());
        }
        RETAIN_STRING.add(CodeIndexAttribute.tag);
        RETAIN_STRING.add(SecretBoxAttribute.tag);
    }

    public static byte[] toBytes(ClassTransformInfo info) {
        ConstPool constPool = info.getConstPool();
        Set<Integer> retainConst = info.getRetainConst();
        ByteArrayOutputStream buff = new ByteArrayOutputStream(
                (constPool.getSize() - retainConst.size()) * CONST_INFO_SIZE_ESTIMATE + info.getCodesLen() + info.codesSize() * 4);
        try {
            constPoolToBytes(info, buff);
            buff.write(new byte[] {0,0,0});
            for (byte[] code : info.getCodes()) {
                buff.write(intToBytes(code.length));
                buff.write(code);
            }
        } catch (Exception e) {
            throw new RuntimeException("cannot convert to byte", e);
        }
        return buff.toByteArray();
    }

    public static byte[] intToBytes(int i) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(i);
        return buffer.array();
    }
    public static void constPoolToBytes(ClassTransformInfo info, ByteArrayOutputStream buff) throws IOException {
        Random random = new Random();
        ConstPool constPool = info.getConstPool();
        Set<Integer> retainConst = info.getRetainConst();
        for (int i = 0; i < constPool.getSize(); i++) {
            if (retainConst.contains(i)) {
                continue;
            }
            ConstInfo item = constPool.getItem(i);
            int len;
            byte type;
            Consumer<ByteBuffer> put;
            switch (item.getTag()) {
                case ConstPool.CONST_Integer:
                    IntegerInfo integerInfo = (IntegerInfo) item;
                    len = Integer.BYTES + CONST_INFO_PREFIX_SIZE;
                    type = ConstBaseType.Integer.getType();
                    put = bb -> bb.putInt(integerInfo.value);
                    integerInfo.value += random.nextInt() >> 1 + 1;
                    break;
                case ConstPool.CONST_Float:
                    FloatInfo floatInfo = (FloatInfo) item;
                    len = Float.BYTES + CONST_INFO_PREFIX_SIZE;
                    type = ConstBaseType.Float.getType();
                    put = bb -> bb.putFloat(floatInfo.value);
                    floatInfo.value += random.nextInt() >> 1 + 1;
                    break;
                case ConstPool.CONST_Long:
                    LongInfo longInfo = (LongInfo) item;
                    len = Long.BYTES + CONST_INFO_PREFIX_SIZE;
                    type = ConstBaseType.Long.getType();
                    put = bb -> bb.putLong(longInfo.value);
                    longInfo.value += random.nextInt() + 1;
                    break;
                case ConstPool.CONST_Double:
                    DoubleInfo doubleInfo = (DoubleInfo) item;
                    len = Double.BYTES + CONST_INFO_PREFIX_SIZE;
                    type = ConstBaseType.Double.getType();
                    put = bb -> bb.putDouble(doubleInfo.value);
                    doubleInfo.value += random.nextInt() + 1;
                    break;
                case ConstPool.CONST_Utf8:
                    Utf8Info utf8Info = (Utf8Info) item;
                    if (RETAIN_STRING.contains(utf8Info.string)) {
                        continue;
                    }
                    len = utf8Info.string.length() + Integer.BYTES + CONST_INFO_PREFIX_SIZE;
                    type = ConstBaseType.String.getType();
                    put = bb -> {
                        byte[] bytes = utf8Info.string.getBytes(StandardCharsets.UTF_8);
                        bb.putInt(bytes.length);
                        bb.put(bytes);
                    };
                    utf8Info.string = "***";
                    break;
                default:
                    continue;
            }

            ByteBuffer bb;
            bb = ByteBuffer.allocate(len);
            bb.put(CONST_INFO_INDEX_PREFIX);
            bb.put(type);
            put.accept(bb);
            byte[] array = bb.array();
            ByteArray.write16bit(item.index, array, 0);
            buff.write(array);
        }
    }
}
