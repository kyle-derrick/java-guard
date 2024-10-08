package javassist.bytecode;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/08 10:32
 */
public class CodeIndexAttribute extends AttributeInfo {
    /**
     * The name of this attribute <code>"<CodeIndex>"</code>.
     */
    public static final String tag = "<CodeIndex>";

    public CodeIndexAttribute(ConstPool cp, int index) {
        this(cp);
        this.setIndex(index);
    }

    public CodeIndexAttribute(ConstPool cp) {
        super(cp, tag);
    }

    public void setIndex(int index) {
        byte[] bytes = new byte[4];
        ByteArray.write32bit(index, bytes, 0);
        this.set(bytes);
    }

    public int getIndex() {
        byte[] bytes = this.get();
        return ByteArray.read32bit(bytes, 0);
    }
}
