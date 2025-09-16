package io.kyle.javaguard.bean;

import javassist.bytecode.CodeAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.JavassistExt;

import java.util.*;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/08 10:32
 */
public class ClassTransformInfo {
    private final ConstPool constPool;
    private final Set<Integer> retainConst = new HashSet<>();
    private final List<CodeAttribute> codes = new LinkedList<>();
    private int codesBytes = 0;

    public ClassTransformInfo(ConstPool constPool) {
        this.constPool = constPool;
    }

    public ConstPool getConstPool() {
        return constPool;
    }

    public Set<Integer> getRetainConst() {
        return retainConst;
    }

    public int getCodesBytes() {
        return codesBytes;
    }

    public List<CodeAttribute> getCodes() {
        return Collections.unmodifiableList(codes);
    }

    public int codesSize() {
        return codes.size();
    }

    public void addCode(CodeAttribute code) {
        this.codes.add(code);
        if (code != null) {
            codesBytes += code.getCodeLength();
        }
    }

    public void addRetainConst(int index) {
        JavassistExt.retainConst(constPool, index, retainConst);
    }

}
