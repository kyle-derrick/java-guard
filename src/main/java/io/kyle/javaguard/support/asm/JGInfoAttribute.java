package io.kyle.javaguard.support.asm;

import io.kyle.javaguard.constant.ConstVars;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassWriter;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2026/3/31 10:53
 */
public class JGInfoAttribute extends Attribute {
    public static final String tag = "<JGInfo>";
    private final byte[] data;
    private final int off;
    private final int len;
    private boolean reserveSign;
    /**
     * Constructs a new empty attribute.
     * encrypt data + (sign(64)) + (hasSign(1)) + magic(5)
     */
    public JGInfoAttribute(byte[] data, int off, int len, boolean reserveSign) {
        super(tag);
        this.data = data;
        this.off = off;
        this.len = len;
        this.reserveSign = reserveSign;
    }
    public JGInfoAttribute(byte[] data) {
        this(data, 0, data.length, false);
    }
    public JGInfoAttribute(byte[] data, int off, int len) {
        this(data, off, len, false);
    }

    @Override
    protected ByteVector write(ClassWriter classWriter, byte[] code, int codeLength, int maxStack, int maxLocals) {
        ByteVector vector = new ByteVector();
        vector.putByteArray(data, off, len);
        byte[] suffix = ConstVars.ENCRYPT_CLASS_SUFFIX;
        vector.putInt(len);
        if (reserveSign) {
            vector.putByteArray(new byte[ConstVars.SIGN_LENGTH], 0, ConstVars.SIGN_LENGTH);
            suffix[0] |= ConstVars.BYTE_H_SIGN;
        }
        vector.putByteArray(suffix, 0, suffix.length);
        return vector;
    }

    public JGInfoAttribute reserveSign(boolean reserveSign) {
        this.reserveSign = reserveSign;
        return this;
    }
}
