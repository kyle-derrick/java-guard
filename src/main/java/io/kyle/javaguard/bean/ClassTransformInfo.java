package io.kyle.javaguard.bean;

import javassist.bytecode.ClassTransformUtils;
import javassist.bytecode.ConstPool;
import javassist.bytecode.JavassistExt;

import java.util.*;

public class ClassTransformInfo {
    private final ConstPool constPool;
    private final Set<Integer> retainConst = new HashSet<>();
    private final List<byte[]> codes = new LinkedList<>();
    private int codesLen = 0;

    public ClassTransformInfo(ConstPool constPool) {
        this.constPool = constPool;
    }

    public ConstPool getConstPool() {
        return constPool;
    }

    public Set<Integer> getRetainConst() {
        return retainConst;
    }

    public int getCodesLen() {
        return codesLen;
    }

    public List<byte[]> getCodes() {
        return Collections.unmodifiableList(codes);
    }

    public int codesSize() {
        return codes.size();
    }

    public void addCode(byte[] code) {
        this.codes.add(code);
        codesLen+=code.length;
    }

    public void addRetainConst(int index) {
        JavassistExt.retainConst(constPool, index, retainConst);
    }

}
