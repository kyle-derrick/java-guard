/* Opcodes */
pub mod opcodes {
    pub const AALOAD: u8 = 50;
    pub const AASTORE: u8 = 83;
    pub const ACONST_NULL: u8 = 1;
    pub const ALOAD: u8 = 25;
    pub const ALOAD_0: u8 = 42;
    pub const ALOAD_1: u8 = 43;
    pub const ALOAD_2: u8 = 44;
    pub const ALOAD_3: u8 = 45;
    pub const ANEWARRAY: u8 = 189;
    pub const ARETURN: u8 = 176;
    pub const ARRAYLENGTH: u8 = 190;
    pub const ASTORE: u8 = 58;
    pub const ASTORE_0: u8 = 75;
    pub const ASTORE_1: u8 = 76;
    pub const ASTORE_2: u8 = 77;
    pub const ASTORE_3: u8 = 78;
    pub const ATHROW: u8 = 191;
    pub const BALOAD: u8 = 51;
    pub const BASTORE: u8 = 84;
    pub const BIPUSH: u8 = 16;
    pub const CALOAD: u8 = 52;
    pub const CASTORE: u8 = 85;
    pub const CHECKCAST: u8 = 192;
    pub const D2F: u8 = 144;
    pub const D2I: u8 = 142;
    pub const D2L: u8 = 143;
    pub const DADD: u8 = 99;
    pub const DALOAD: u8 = 49;
    pub const DASTORE: u8 = 82;
    pub const DCMPG: u8 = 152;
    pub const DCMPL: u8 = 151;
    pub const DCONST_0: u8 = 14;
    pub const DCONST_1: u8 = 15;
    pub const DDIV: u8 = 111;
    pub const DLOAD: u8 = 24;
    pub const DLOAD_0: u8 = 38;
    pub const DLOAD_1: u8 = 39;
    pub const DLOAD_2: u8 = 40;
    pub const DLOAD_3: u8 = 41;
    pub const DMUL: u8 = 107;
    pub const DNEG: u8 = 119;
    pub const DREM: u8 = 115;
    pub const DRETURN: u8 = 175;
    pub const DSTORE: u8 = 57;
    pub const DSTORE_0: u8 = 71;
    pub const DSTORE_1: u8 = 72;
    pub const DSTORE_2: u8 = 73;
    pub const DSTORE_3: u8 = 74;
    pub const DSUB: u8 = 103;
    pub const DUP: u8 = 89;
    pub const DUP2: u8 = 92;
    pub const DUP2_X1: u8 = 93;
    pub const DUP2_X2: u8 = 94;
    pub const DUP_X1: u8 = 90;
    pub const DUP_X2: u8 = 91;
    pub const F2D: u8 = 141;
    pub const F2I: u8 = 139;
    pub const F2L: u8 = 140;
    pub const FADD: u8 = 98;
    pub const FALOAD: u8 = 48;
    pub const FASTORE: u8 = 81;
    pub const FCMPG: u8 = 150;
    pub const FCMPL: u8 = 149;
    pub const FCONST_0: u8 = 11;
    pub const FCONST_1: u8 = 12;
    pub const FCONST_2: u8 = 13;
    pub const FDIV: u8 = 110;
    pub const FLOAD: u8 = 23;
    pub const FLOAD_0: u8 = 34;
    pub const FLOAD_1: u8 = 35;
    pub const FLOAD_2: u8 = 36;
    pub const FLOAD_3: u8 = 37;
    pub const FMUL: u8 = 106;
    pub const FNEG: u8 = 118;
    pub const FREM: u8 = 114;
    pub const FRETURN: u8 = 174;
    pub const FSTORE: u8 = 56;
    pub const FSTORE_0: u8 = 67;
    pub const FSTORE_1: u8 = 68;
    pub const FSTORE_2: u8 = 69;
    pub const FSTORE_3: u8 = 70;
    pub const FSUB: u8 = 102;
    pub const GETFIELD: u8 = 180;
    pub const GETSTATIC: u8 = 178;
    pub const GOTO: u8 = 167;
    pub const GOTO_W: u8 = 200;
    pub const I2B: u8 = 145;
    pub const I2C: u8 = 146;
    pub const I2D: u8 = 135;
    pub const I2F: u8 = 134;
    pub const I2L: u8 = 133;
    pub const I2S: u8 = 147;
    pub const IADD: u8 = 96;
    pub const IALOAD: u8 = 46;
    pub const IAND: u8 = 126;
    pub const IASTORE: u8 = 79;
    pub const ICONST_0: u8 = 3;
    pub const ICONST_1: u8 = 4;
    pub const ICONST_2: u8 = 5;
    pub const ICONST_3: u8 = 6;
    pub const ICONST_4: u8 = 7;
    pub const ICONST_5: u8 = 8;
    pub const ICONST_M1: u8 = 2;
    pub const IDIV: u8 = 108;
    pub const IFEQ: u8 = 153;
    pub const IFGE: u8 = 156;
    pub const IFGT: u8 = 157;
    pub const IFLE: u8 = 158;
    pub const IFLT: u8 = 155;
    pub const IFNE: u8 = 154;
    pub const IFNONNULL: u8 = 199;
    pub const IFNULL: u8 = 198;
    pub const IF_ACMPEQ: u8 = 165;
    pub const IF_ACMPNE: u8 = 166;
    pub const IF_ICMPEQ: u8 = 159;
    pub const IF_ICMPGE: u8 = 162;
    pub const IF_ICMPGT: u8 = 163;
    pub const IF_ICMPLE: u8 = 164;
    pub const IF_ICMPLT: u8 = 161;
    pub const IF_ICMPNE: u8 = 160;
    pub const IINC: u8 = 132;
    pub const ILOAD: u8 = 21;
    pub const ILOAD_0: u8 = 26;
    pub const ILOAD_1: u8 = 27;
    pub const ILOAD_2: u8 = 28;
    pub const ILOAD_3: u8 = 29;
    pub const IMUL: u8 = 104;
    pub const INEG: u8 = 116;
    pub const INSTANCEOF: u8 = 193;
    pub const INVOKEDYNAMIC: u8 = 186;
    pub const INVOKEINTERFACE: u8 = 185;
    pub const INVOKESPECIAL: u8 = 183;
    pub const INVOKESTATIC: u8 = 184;
    pub const INVOKEVIRTUAL: u8 = 182;
    pub const IOR: u8 = 128;
    pub const IREM: u8 = 112;
    pub const IRETURN: u8 = 172;
    pub const ISHL: u8 = 120;
    pub const ISHR: u8 = 122;
    pub const ISTORE: u8 = 54;
    pub const ISTORE_0: u8 = 59;
    pub const ISTORE_1: u8 = 60;
    pub const ISTORE_2: u8 = 61;
    pub const ISTORE_3: u8 = 62;
    pub const ISUB: u8 = 100;
    pub const IUSHR: u8 = 124;
    pub const IXOR: u8 = 130;
    pub const JSR: u8 = 168;
    pub const JSR_W: u8 = 201;
    pub const L2D: u8 = 138;
    pub const L2F: u8 = 137;
    pub const L2I: u8 = 136;
    pub const LADD: u8 = 97;
    pub const LALOAD: u8 = 47;
    pub const LAND: u8 = 127;
    pub const LASTORE: u8 = 80;
    pub const LCMP: u8 = 148;
    pub const LCONST_0: u8 = 9;
    pub const LCONST_1: u8 = 10;
    pub const LDC: u8 = 18;
    pub const LDC2_W: u8 = 20;
    pub const LDC_W: u8 = 19;
    pub const LDIV: u8 = 109;
    pub const LLOAD: u8 = 22;
    pub const LLOAD_0: u8 = 30;
    pub const LLOAD_1: u8 = 31;
    pub const LLOAD_2: u8 = 32;
    pub const LLOAD_3: u8 = 33;
    pub const LMUL: u8 = 105;
    pub const LNEG: u8 = 117;
    pub const LOOKUPSWITCH: u8 = 171;
    pub const LOR: u8 = 129;
    pub const LREM: u8 = 113;
    pub const LRETURN: u8 = 173;
    pub const LSHL: u8 = 121;
    pub const LSHR: u8 = 123;
    pub const LSTORE: u8 = 55;
    pub const LSTORE_0: u8 = 63;
    pub const LSTORE_1: u8 = 64;
    pub const LSTORE_2: u8 = 65;
    pub const LSTORE_3: u8 = 66;
    pub const LSUB: u8 = 101;
    pub const LUSHR: u8 = 125;
    pub const LXOR: u8 = 131;
    pub const MONITORENTER: u8 = 194;
    pub const MONITOREXIT: u8 = 195;
    pub const MULTIANEWARRAY: u8 = 197;
    pub const NEW: u8 = 187;
    pub const NEWARRAY: u8 = 188;
    pub const NOP: u8 = 0;
    pub const POP: u8 = 87;
    pub const POP2: u8 = 88;
    pub const PUTFIELD: u8 = 181;
    pub const PUTSTATIC: u8 = 179;
    pub const RET: u8 = 169;
    pub const RETURN: u8 = 177;
    pub const SALOAD: u8 = 53;
    pub const SASTORE: u8 = 86;
    pub const SIPUSH: u8 = 17;
    pub const SWAP: u8 = 95;
    pub const TABLESWITCH: u8 = 170;
    pub const WIDE: u8 = 196;
}

/* array-type code for the newarray instruction */
pub mod array_type {
    pub const T_BOOLEAN: u8 = 4;
    pub const T_CHAR: u8 = 5;
    pub const T_FLOAT: u8 = 6;
    pub const T_DOUBLE: u8 = 7;
    pub const T_BYTE: u8 = 8;
    pub const T_SHORT: u8 = 9;
    pub const T_INT: u8 = 10;
    pub const T_LONG: u8 = 11;
}

/* how many values are pushed on the operand stack. */
pub const STACK_GROW: &[i16] = &[
    0, // nop, 0
    1, // aconst_null, 1
    1, // iconst_m1, 2
    1, // iconst_0, 3
    1, // iconst_1, 4
    1, // iconst_2, 5
    1, // iconst_3, 6
    1, // iconst_4, 7
    1, // iconst_5, 8
    2, // lconst_0, 9
    2, // lconst_1, 10
    1, // fconst_0, 11
    1, // fconst_1, 12
    1, // fconst_2, 13
    2, // dconst_0, 14
    2, // dconst_1, 15
    1, // bipush, 16
    1, // sipush, 17
    1, // ldc, 18
    1, // ldc_w, 19
    2, // ldc2_w, 20
    1, // iload, 21
    2, // lload, 22
    1, // fload, 23
    2, // dload, 24
    1, // aload, 25
    1, // iload_0, 26
    1, // iload_1, 27
    1, // iload_2, 28
    1, // iload_3, 29
    2, // lload_0, 30
    2, // lload_1, 31
    2, // lload_2, 32
    2, // lload_3, 33
    1, // fload_0, 34
    1, // fload_1, 35
    1, // fload_2, 36
    1, // fload_3, 37
    2, // dload_0, 38
    2, // dload_1, 39
    2, // dload_2, 40
    2, // dload_3, 41
    1, // aload_0, 42
    1, // aload_1, 43
    1, // aload_2, 44
    1, // aload_3, 45
    -1, // iaload, 46
    0, // laload, 47
    -1, // faload, 48
    0, // daload, 49
    -1, // aaload, 50
    -1, // baload, 51
    -1, // caload, 52
    -1, // saload, 53
    -1, // istore, 54
    -2, // lstore, 55
    -1, // fstore, 56
    -2, // dstore, 57
    -1, // astore, 58
    -1, // istore_0, 59
    -1, // istore_1, 60
    -1, // istore_2, 61
    -1, // istore_3, 62
    -2, // lstore_0, 63
    -2, // lstore_1, 64
    -2, // lstore_2, 65
    -2, // lstore_3, 66
    -1, // fstore_0, 67
    -1, // fstore_1, 68
    -1, // fstore_2, 69
    -1, // fstore_3, 70
    -2, // dstore_0, 71
    -2, // dstore_1, 72
    -2, // dstore_2, 73
    -2, // dstore_3, 74
    -1, // astore_0, 75
    -1, // astore_1, 76
    -1, // astore_2, 77
    -1, // astore_3, 78
    -3, // iastore, 79
    -4, // lastore, 80
    -3, // fastore, 81
    -4, // dastore, 82
    -3, // aastore, 83
    -3, // bastore, 84
    -3, // castore, 85
    -3, // sastore, 86
    -1, // pop, 87
    -2, // pop2, 88
    1, // dup, 89
    1, // dup_x1, 90
    1, // dup_x2, 91
    2, // dup2, 92
    2, // dup2_x1, 93
    2, // dup2_x2, 94
    0, // swap, 95
    -1, // iadd, 96
    -2, // ladd, 97
    -1, // fadd, 98
    -2, // dadd, 99
    -1, // isub, 100
    -2, // lsub, 101
    -1, // fsub, 102
    -2, // dsub, 103
    -1, // imul, 104
    -2, // lmul, 105
    -1, // fmul, 106
    -2, // dmul, 107
    -1, // idiv, 108
    -2, // ldiv, 109
    -1, // fdiv, 110
    -2, // ddiv, 111
    -1, // irem, 112
    -2, // lrem, 113
    -1, // frem, 114
    -2, // drem, 115
    0, // ineg, 116
    0, // lneg, 117
    0, // fneg, 118
    0, // dneg, 119
    -1, // ishl, 120
    -1, // lshl, 121
    -1, // ishr, 122
    -1, // lshr, 123
    -1, // iushr, 124
    -1, // lushr, 125
    -1, // iand, 126
    -2, // land, 127
    -1, // ior, 128
    -2, // lor, 129
    -1, // ixor, 130
    -2, // lxor, 131
    0, // iinc, 132
    1, // i2l, 133
    0, // i2f, 134
    1, // i2d, 135
    -1, // l2i, 136
    -1, // l2f, 137
    0, // l2d, 138
    0, // f2i, 139
    1, // f2l, 140
    1, // f2d, 141
    -1, // d2i, 142
    0, // d2l, 143
    -1, // d2f, 144
    0, // i2b, 145
    0, // i2c, 146
    0, // i2s, 147
    -3, // lcmp, 148
    -1, // fcmpl, 149
    -1, // fcmpg, 150
    -3, // dcmpl, 151
    -3, // dcmpg, 152
    -1, // ifeq, 153
    -1, // ifne, 154
    -1, // iflt, 155
    -1, // ifge, 156
    -1, // ifgt, 157
    -1, // ifle, 158
    -2, // if_icmpeq, 159
    -2, // if_icmpne, 160
    -2, // if_icmplt, 161
    -2, // if_icmpge, 162
    -2, // if_icmpgt, 163
    -2, // if_icmple, 164
    -2, // if_acmpeq, 165
    -2, // if_acmpne, 166
    0, // goto, 167
    1, // jsr, 168
    0, // ret, 169
    -1, // tableswitch, 170
    -1, // lookupswitch, 171
    -1, // ireturn, 172
    -2, // lreturn, 173
    -1, // freturn, 174
    -2, // dreturn, 175
    -1, // areturn, 176
    0, // return, 177
    0, // getstatic, 178            depends on the type
    0, // putstatic, 179            depends on the type
    0, // getfield, 180             depends on the type
    0, // putfield, 181             depends on the type
    0, // invokevirtual, 182        depends on the type
    0, // invokespecial, 183        depends on the type
    0, // invokestatic, 184         depends on the type
    0, // invokeinterface, 185      depends on the type
    0, // invokedynaimc, 186        depends on the type
    1, // new, 187
    0, // newarray, 188
    0, // anewarray, 189
    0, // arraylength, 190
    -1, // athrow, 191              stack is cleared
    0, // checkcast, 192
    0, // instanceof, 193
    -1, // monitorenter, 194
    -1, // monitorexit, 195
    0, // wide, 196                 depends on the following opcode
    0, // multianewarray, 197       depends on the dimensions
    -1, // ifnull, 198
    -1, // ifnonnull, 199
    0, // goto_w, 200
    1 // jsr_w, 201
];
