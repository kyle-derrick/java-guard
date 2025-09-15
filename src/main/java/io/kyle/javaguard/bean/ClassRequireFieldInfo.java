package io.kyle.javaguard.bean;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2025/9/15 18:01
 */
public class ClassRequireFieldInfo {
    private int refIndex;
    public final String name;
    public final String signature;
    public final boolean isStatic;

    public ClassRequireFieldInfo(String name, String signature, boolean isStatic) {
        this.name = name;
        this.signature = signature;
        this.isStatic = isStatic;
    }

    public int getRefIndex() {
        return refIndex;
    }

    public void setRefIndex(int refIndex) {
        this.refIndex = refIndex;
    }
}
