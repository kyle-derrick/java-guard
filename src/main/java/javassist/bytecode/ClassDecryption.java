package javassist.bytecode;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public class ClassDecryption {

    public static final byte CLASS_ENCRYPT_FLAG = (byte) (1 << 7);

    public static final byte CLASS_ENCRYPT_TRANS_FLAG = (byte) (~CLASS_ENCRYPT_FLAG);

    public static byte[] decryptClass(byte[] data) {
        try {
            return decryptClass(data, ClassDecryption::decryptData);
        } catch (Exception e) {
            System.err.println("decrypt class failed");
            e.printStackTrace();
            return data;
        }
    }

    private static native byte[] decryptData(byte[] data);

    public static byte[] decryptClass(byte[] data, Function<byte[], byte[]> decrypter) throws IOException {
        if (data.length < 4) {
            return data;
        }
        byte flag = data[4];
        if ((flag & CLASS_ENCRYPT_FLAG) == 0) {
            return data;
        }
        data[4] &= CLASS_ENCRYPT_TRANS_FLAG;
        ClassFile classFile = new ClassFile(new DataInputStream(new ByteArrayInputStream(data)));
        AttributeInfo secretBoxAttribute = classFile.getAttribute(SecretBoxAttribute.tag);
        if (secretBoxAttribute == null) {
            return data;
        }
        classFile.removeAttribute(SecretBoxAttribute.tag);
        byte[] codeData = decrypter.apply(secretBoxAttribute.get());
        ByteBuffer buffer = ByteBuffer.wrap(codeData);
        byte[] prefix = new byte[3];
        HashMap<Integer, Object> decryptConst = new HashMap<>();
        while (buffer.position() < buffer.capacity()) {
            buffer.get(prefix);
            int index = ByteArray.readU16bit(prefix, 0);
            if (prefix[2] == 0) {
                break;
            }
            Object value;
            switch (prefix[2]) {
                case 'I':
                    value = buffer.getInt();
                    break;
                case 'F':
                    value = buffer.getFloat();
                    break;
                case 'L':
                    value = buffer.getLong();
                    break;
                case 'D':
                    value = buffer.getDouble();
                    break;
                case 'S':
                    int len = buffer.getInt();
                    byte[] bytes = new byte[len];
                    buffer.get(bytes);
                    value = new String(bytes, StandardCharsets.UTF_8);
                    break;
                default:
                    continue;
            }
            decryptConst.put(index, value);
        }
        int codesSize = buffer.getInt();
        List<byte[]> codes = new ArrayList<>(codesSize);
        while (buffer.position() < buffer.capacity()) {
            int len = buffer.getInt();
            byte[] bytes = new byte[len];
            buffer.get(bytes);
            codes.add(bytes);
        }
        ConstPool constPool = classFile.getConstPool();
        decryptConst.forEach((index, value) -> {
            ConstInfo item = constPool.getItem(index);
            if (item == null) {
                return;
            }
            switch (item.getTag()) {
                case ConstPool.CONST_Integer:
                    ((IntegerInfo)item).value = (Integer) value;
                    break;
                case ConstPool.CONST_Float:
                    ((FloatInfo)item).value = (Float) value;
                    break;
                case ConstPool.CONST_Long:
                    ((LongInfo)item).value = (Long) value;
                    break;
                case ConstPool.CONST_Double:
                    ((DoubleInfo)item).value = (Double) value;
                    break;
                case ConstPool.CONST_Utf8:
                    ((Utf8Info)item).string = (String) value;
                    break;
                default:
            }
        });

        for (MethodInfo method : classFile.getMethods()) {
            CodeAttribute codeAttribute = method.getCodeAttribute();
            if (codeAttribute == null) {
                continue;
            }
            AttributeInfo codeIndexAttribute = codeAttribute.getAttribute(CodeIndexAttribute.tag);
            if (codeIndexAttribute == null) {
                continue;
            }
            int index = ByteArray.read32bit(codeIndexAttribute.get(), 0);
            byte[] code = codes.get(index);
            CodeAttribute newCodeAttribute = new CodeAttribute(codeAttribute.getConstPool(), codeAttribute.getMaxStack(), codeAttribute.getMaxLocals(),
                    code, codeAttribute.getExceptionTable());
            List<AttributeInfo> attributes = codeAttribute.getAttributes();
            attributes.remove(codeIndexAttribute);
            newCodeAttribute.getAttributes().addAll(attributes);
            method.setCodeAttribute(newCodeAttribute);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
        classFile.write(new DataOutputStream(out));
        return out.toByteArray();
    }
}
