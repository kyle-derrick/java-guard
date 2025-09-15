package io.kyle.javaguard.bean;

import java.util.List;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2025/9/15 18:00
 */
public class ClassRequiredInfos {
    public final List<ClassRequireFieldInfo> fields;
    public final List<ClassRequireFieldInfo> staticFields;

    public ClassRequiredInfos(List<ClassRequireFieldInfo> fields, List<ClassRequireFieldInfo> staticFields) {
        this.fields = fields;
        this.staticFields = staticFields;
    }
}
