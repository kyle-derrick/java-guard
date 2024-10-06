package io.kyle.javaguard.constant;

public enum ConstBaseType {
    /**
     *
     */
    Integer('I'),
    Float('F'),
    Long('L'),
    Double('D'),
    String('S'),
    ;
    private final byte type;

    ConstBaseType(char type) {
        this.type = (byte) type;
    }

    public ConstBaseType of(byte type) {
        switch (type) {
            case 'I':
                return Integer;
            case 'F':
                return Float;
            case 'L':
                return Long;
            case 'D':
                return Double;
            case 'S':
                return String;
            default:
                return null;
        }
    }

    public byte getType() {
        return type;
    }
}
