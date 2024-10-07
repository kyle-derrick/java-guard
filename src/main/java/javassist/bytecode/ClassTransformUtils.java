package javassist.bytecode;

import io.kyle.javaguard.bean.ClassTransformInfo;
import io.kyle.javaguard.constant.ClassAttribute;
import io.kyle.javaguard.constant.ConstBaseType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
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
            List<byte[]> codes = info.getCodes();
            buff.write(intToBytes(codes.size()));
            for (byte[] code : codes) {
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
            if (item == null) {
                continue;
            }
            int len;
            byte type;
            Consumer<ByteBuffer> put;
            switch (item.getTag()) {
                case ConstPool.CONST_Integer:
                    IntegerInfo integerInfo = (IntegerInfo) item;
                    len = Integer.BYTES;
                    type = ConstBaseType.Integer.getType();
                    int iv = integerInfo.value;
                    put = bb -> bb.putInt(iv);
                    integerInfo.value += random.nextInt() >> 1 + 1;
                    break;
                case ConstPool.CONST_Float:
                    FloatInfo floatInfo = (FloatInfo) item;
                    len = Float.BYTES;
                    type = ConstBaseType.Float.getType();
                    float fv = floatInfo.value;
                    put = bb -> bb.putFloat(fv);
                    floatInfo.value += random.nextInt() >> 1 + 1;
                    break;
                case ConstPool.CONST_Long:
                    LongInfo longInfo = (LongInfo) item;
                    len = Long.BYTES;
                    type = ConstBaseType.Long.getType();
                    long lv = longInfo.value;
                    put = bb -> bb.putLong(lv);
                    longInfo.value += random.nextInt() + 1;
                    break;
                case ConstPool.CONST_Double:
                    DoubleInfo doubleInfo = (DoubleInfo) item;
                    len = Double.BYTES;
                    type = ConstBaseType.Double.getType();
                    double dv = doubleInfo.value;
                    put = bb -> bb.putDouble(dv);
                    doubleInfo.value += random.nextInt() + 1;
                    break;
                case ConstPool.CONST_Utf8:
                    Utf8Info utf8Info = (Utf8Info) item;
                    String string = utf8Info.string;
                    if (RETAIN_STRING.contains(string)) {
                        continue;
                    }
                    byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
                    len = bytes.length + Integer.BYTES;
                    type = ConstBaseType.String.getType();
                    put = bb -> {
                        bb.putInt(bytes.length);
                        bb.put(bytes);
                    };
                    utf8Info.string = "***";
                    break;
                default:
                    continue;
            }

            ByteBuffer bb;
            bb = ByteBuffer.allocate(len + CONST_INFO_PREFIX_SIZE);
            bb.put(CONST_INFO_INDEX_PREFIX);
            bb.put(type);
            put.accept(bb);
            byte[] array = bb.array();
            ByteArray.write16bit(item.index, array, 0);
            buff.write(array);
        }
    }
}
