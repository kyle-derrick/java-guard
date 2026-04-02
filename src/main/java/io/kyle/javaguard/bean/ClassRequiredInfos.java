package io.kyle.javaguard.bean;

import org.objectweb.asm.tree.FieldNode;

import java.util.List;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2025/9/15 18:00
 */
public class ClassRequiredInfos {
    public final List<FieldNode> fields;
    public final List<FieldNode> staticFields;

    public ClassRequiredInfos(List<FieldNode> fields, List<FieldNode> staticFields) {
        this.fields = fields;
        this.staticFields = staticFields;
    }
}
