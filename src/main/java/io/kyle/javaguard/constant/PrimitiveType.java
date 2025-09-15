package io.kyle.javaguard.constant;

import javassist.bytecode.Descriptor;
import javassist.bytecode.Opcode;
import org.apache.commons.lang3.StringUtils;

public enum PrimitiveType {
    booleanType(Opcode.ICONST_0, Opcode.IRETURN),
    charType(Opcode.ICONST_0, Opcode.IRETURN),
    byteType(Opcode.ICONST_0, Opcode.IRETURN),
    shortType(Opcode.ICONST_0, Opcode.IRETURN),
    intType(Opcode.ICONST_0, Opcode.IRETURN),
    longType(Opcode.LCONST_0, Opcode.LRETURN),
    floatType(Opcode.FCONST_0, Opcode.FRETURN),
    doubleType(Opcode.DCONST_0, Opcode.DRETURN),
    voidType(Opcode.NOP, Opcode.RETURN),
    objectType(Opcode.ACONST_NULL, Opcode.ARETURN),
    ;

    public final int defaultValue;
    public final int returnOpcode;

    public static final char BOOLEAN_TYPE = 'Z';
    public static final char CHAR_TYPE = 'C';
    public static final char BYTE_TYPE = 'B';
    public static final char SHORT_TYPE = 'S';
    public static final char INT_TYPE = 'I';
    public static final char LONG_TYPE = 'J';
    public static final char FLOAT_TYPE = 'F';
    public static final char DOUBLE_TYPE = 'D';
    public static final char VOID_TYPE = 'V';

    PrimitiveType(int defaultValue, int returnOpcode) {
        this.defaultValue = defaultValue;
        this.returnOpcode = returnOpcode;
    }

    public static PrimitiveType returnType(String desc) {
        if (StringUtils.isEmpty(desc)) {
            throw new IllegalArgumentException("desc is empty");
        }
        int i = StringUtils.indexOf(desc, ')');
        if (i != -1) {
            return from(desc.charAt(i + 1));
        }
        return from(desc.charAt(0));
    }

    public static PrimitiveType[] paramTypes(String desc) {
        if (StringUtils.isEmpty(desc) || desc.charAt(0) != '(') {
            throw new IllegalArgumentException("desc is empty");
        }
        int size = Descriptor.numOfParameters(desc);
        PrimitiveType[] types = new PrimitiveType[size];
        int n = 0;
        int i = 1;
        do {
            i = toTypes(desc, i, types, n++);
        } while (i > 0);
        return types;
    }

    private static int toTypes(String desc, int i,
                               PrimitiveType[] types, int n) {
        int i2;

        char c = desc.charAt(i);
        if (c == ')') {
            return -1;
        }
        PrimitiveType type = from(c);
        while (c == '[') {
            c = desc.charAt(++i);
        }

        if (c == 'L') {
            i2 = desc.indexOf(';', ++i);
        }
        else {
            i2 = i;
        }

        types[n] = type;
        return i2 + 1;
    }

    public static PrimitiveType from(char code) {
        switch (code) {
            case BOOLEAN_TYPE :
                return booleanType;
            case CHAR_TYPE :
                return charType;
            case BYTE_TYPE :
                return byteType;
            case SHORT_TYPE :
                return shortType;
            case INT_TYPE :
                return intType;
            case LONG_TYPE :
                return longType;
            case FLOAT_TYPE :
                return floatType;
            case DOUBLE_TYPE :
                return doubleType;
            case VOID_TYPE :
                return voidType;
            default:
                return objectType;
        }
    }
}
