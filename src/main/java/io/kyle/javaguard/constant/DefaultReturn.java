package io.kyle.javaguard.constant;

import javassist.CtClass;
import javassist.bytecode.Opcode;

public enum DefaultReturn {
    /**
     *
     */
    BOOLEAN(Opcode.ICONST_0, Opcode.IRETURN),
    CHAR(Opcode.ICONST_0, Opcode.IRETURN),
    BYTE(Opcode.ICONST_0, Opcode.IRETURN),
    SHORT(Opcode.ICONST_0, Opcode.IRETURN),
    INT(Opcode.ICONST_0, Opcode.IRETURN),
    LONG(Opcode.LCONST_0, Opcode.LRETURN),
    FLOAT(Opcode.FCONST_0, Opcode.FRETURN),
    DOUBLE(Opcode.DCONST_0, Opcode.DRETURN),
    OBJECT(Opcode.ACONST_NULL, Opcode.ARETURN),
    VOID(Opcode.RETURN),
    ;
    private final int[] opcodes;

    DefaultReturn(int ... opcodes) {
        this.opcodes = opcodes;
    }

    public int[] getOpcodes() {
        return opcodes;
    }

    public static DefaultReturn byDescriptor(String descriptor) {
        if (descriptor == null) {
            return OBJECT;
        }
        if (descriptor.charAt(descriptor.length() - 2) == ')') {
            char type = descriptor.charAt(descriptor.length() - 1);
            int[] opcodes;
            switch (type) {
                case 'Z' :
                    return BOOLEAN;
                case 'C' :
                    return CHAR;
                case 'B' :
                    return BYTE;
                case 'S' :
                    return SHORT;
                case 'I' :
                    return INT;
                case 'J' :
                    return LONG;
                case 'F' :
                    return FLOAT;
                case 'D' :
                    return DOUBLE;
                case 'V' :
                    return VOID;
                default :
            }
        }
        return OBJECT;
    }
}
