package javassist.bytecode;

import io.kyle.javaguard.constant.ConstVars;
import io.kyle.javaguard.util.BytesUtils;
import org.apache.commons.lang3.ArrayUtils;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/08 10:32
 */
public class JGExtAttribute extends AttributeInfo {
    /**
     * The name of this attribute <code>"<JGExt>"</code>.
     * signature + 2Byte + suffix
     */
    public static final String tag = "<JGExt>";

    public JGExtAttribute(ConstPool cp, byte[] data) {
        super(cp, tag, generateData(data));
    }

    public JGExtAttribute(ConstPool cp) {
        super(cp, tag, generateData(null));
    }

    private static byte[] generateData(byte[] data) {
        if (ArrayUtils.isEmpty(data)) {
            byte[] bytes = new byte[ConstVars.ENCRYPT_CLASS_SUFFIX.length + Short.BYTES];
            bytes[0] = 0;
            bytes[1] = 0;
            System.arraycopy(ConstVars.ENCRYPT_CLASS_SUFFIX, 0, bytes, 2, ConstVars.ENCRYPT_CLASS_SUFFIX.length);
            return bytes;
        } else {
            byte[] dataLenBytes = BytesUtils.shortToLeBytes((short) data.length);
            byte[] bytes = new byte[data.length + Short.BYTES + ConstVars.ENCRYPT_CLASS_SUFFIX.length];
            System.arraycopy(data, 0, bytes, 0, data.length);
            System.arraycopy(dataLenBytes, 0, bytes, data.length, Short.BYTES);
            System.arraycopy(ConstVars.ENCRYPT_CLASS_SUFFIX, 0, bytes, data.length + Short.BYTES, ConstVars.ENCRYPT_CLASS_SUFFIX.length);
            return bytes;
        }
    }
}
