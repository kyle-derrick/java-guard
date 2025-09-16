package io.kyle.javaguard.util;

import javassist.bytecode.AttributeInfo;

import java.util.List;
import java.util.ListIterator;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2025/9/16 11:06
 */
public class AttributeUtils {
    public static void update(List<AttributeInfo> attributeInfos, AttributeInfo oldAttr, AttributeInfo newAttr) {
        ListIterator<AttributeInfo> iterator = attributeInfos.listIterator();
        while (iterator.hasNext()) {
            AttributeInfo attribute = iterator.next();
            if (attribute == oldAttr) {
                iterator.set(newAttr);
                break;
            }
        }
    }
}
