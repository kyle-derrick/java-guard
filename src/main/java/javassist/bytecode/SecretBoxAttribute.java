package javassist.bytecode;

import java.util.List;

public class SecretBoxAttribute extends AttributeInfo {
    /**
     * The name of this attribute <code>"<SecretBox>"</code>.
     */
    public static final String tag = "<SecretBox>";

    public SecretBoxAttribute(ConstPool cp, byte[] data) {
        super(cp, tag, data);
    }

    public SecretBoxAttribute(ConstPool cp) {
        super(cp, tag);
    }

    public void setCodes(List<byte[]> codes) {
        int len = 0;
    }

}
