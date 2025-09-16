package javassist.bytecode;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2025/9/16 10:56
 */
public class CustomDataAttribute extends AttributeInfo {

    public CustomDataAttribute(ConstPool cp, int attrname, byte[] attrinfo) {
        super(cp, attrname, attrinfo);
    }

    public CustomDataAttribute(AttributeInfo oriAttribute, byte[] attrinfo) {
        super(oriAttribute.getConstPool(), oriAttribute.name, attrinfo);
    }
}
