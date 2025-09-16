package io.kyle.javaguard.util;

import io.kyle.javaguard.bean.ClassRequireFieldInfo;
import io.kyle.javaguard.bean.ClassRequiredInfos;
import io.kyle.javaguard.constant.ConstVars;
import io.kyle.javaguard.constant.PrimitiveType;
import javassist.Modifier;
import javassist.bytecode.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2025/9/15 16:40
 */
public class ClassFileUtils {
    @SuppressWarnings("unused")
    public static ClassRequiredInfos requiredInfos(ClassFile classFile) {
        Map<String, ClassRequireFieldInfo> fieldMap = new HashMap<>();
        int staticCount = 0;
        for (FieldInfo fieldInfo : classFile.getFields()) {
            if (fieldInfo.getConstantValue() != 0) {
                continue;
            }
            int accessFlags = fieldInfo.getAccessFlags();
            if ((AccessFlag.FINAL & accessFlags) == 0) {
                continue;
            }
            boolean isStatic = Modifier.isStatic(accessFlags);
            if (isStatic) {
                staticCount++;
            }
            fieldMap.put(fieldInfo.getName() + ' ' + fieldInfo.getDescriptor(), new ClassRequireFieldInfo(fieldInfo.getName(), fieldInfo.getDescriptor(), isStatic));
        }
        return requiredInfos(classFile, fieldMap, staticCount);
    }

    public static ClassRequiredInfos requiredInfos(ClassFile classFile, Map<String, ClassRequireFieldInfo> fieldMap, int staticCount) {
        ConstPool constPool = classFile.getConstPool();
        String className = classFile.getName();
        ArrayList<ClassRequireFieldInfo> staticFinalFields = new ArrayList<>(staticCount);
        ArrayList<ClassRequireFieldInfo> finalFields = new ArrayList<>(fieldMap.size() - staticCount);
        for (int i = 1; i < constPool.getSize(); i++) {
            if (constPool.getTag(i) == ConstPool.CONST_Fieldref && StringUtils.equals(constPool.getFieldrefClassName(i), className)) {
                ClassRequireFieldInfo fieldInfo = fieldMap.get(constPool.getFieldrefName(i) + ' ' + constPool.getFieldrefType(i));
                if (fieldInfo == null) {
                    continue;
                }
                fieldInfo.setRefIndex(i);
                if (fieldInfo.isStatic) {
                    staticFinalFields.add(fieldInfo);
                } else {
                    finalFields.add(fieldInfo);
                }
            }
        }
        return new ClassRequiredInfos(finalFields, staticFinalFields);
    }

    public static int constructorSuperInvokeMethodRef(ClassFile classFile, ConstPool constPool, CodeAttribute codeAttribute) {
        CodeIterator codeIterator = codeAttribute.iterator();
        int newDepth = 0;
        while (codeIterator.hasNext()) {
            try {
                int index = codeIterator.next();
                int constIndex;
                switch (codeIterator.byteAt(index)) {
                    case Opcode.NEW:
                        newDepth++;
                        continue;
                    case Opcode.INVOKESPECIAL:
                        constIndex = codeIterator.u16bitAt(index + 1);
                        String methodrefName = constPool.getMethodrefName(constIndex);
                        if (ConstVars.CONSTRUCTOR_METHOD_NAME.equals(methodrefName)) {
                            if (newDepth == 0) {
                                break;
                            }
                            newDepth--;
                        }
                        continue;
                    default:
                        continue;

                }
                String methodrefClassName = constPool.getMethodrefClassName(constIndex);
                if (StringUtils.equalsAny(methodrefClassName, classFile.getName(), classFile.getSuperclass())) {
                    return constIndex;
                }
            } catch (BadBytecode e) {
                System.err.println("错误的方法字节码:" + classFile.getName());
            }
        }
        return 0;
    }

    public static void putCode(List<ClassRequireFieldInfo> fields, int putOpcode, Bytecode bytecode) {
        for (ClassRequireFieldInfo fieldInfo : fields) {
            int valueOpcode = PrimitiveType.returnType(fieldInfo.signature).defaultValue;
            if (valueOpcode != Opcode.NOP) {
                bytecode.addOpcode(valueOpcode);
            }
            bytecode.addOpcode(putOpcode);
            bytecode.addIndex(fieldInfo.getRefIndex());
        }
    }

    public static void codeInnerAttributeHandle(int codeLength, CodeAttribute ca, CodeAttribute newCa, ConstPool constPool) {
        List<AttributeInfo> caAttributes = newCa.getAttributes();
        List<AttributeInfo> attributes = ca.getAttributes();
        for (AttributeInfo attr : attributes) {
            LocalVariableAttribute variableAttribute = null;
            LocalVariableAttribute newVariableAttribute = null;
            switch (attr.getName()) {
                case LocalVariableTypeAttribute.tag:
                    variableAttribute = (LocalVariableTypeAttribute) attr;
                    newVariableAttribute = new LocalVariableTypeAttribute(constPool);
                case LocalVariableAttribute.tag:
                    if (variableAttribute == null) {
                        variableAttribute = (LocalVariableAttribute) attr;
                        newVariableAttribute = new LocalVariableAttribute(constPool);
                    }
                    int tableLength = variableAttribute.tableLength();
                    for (int i = 0; i < tableLength; i++) {
                        int startPc = variableAttribute.startPc(i);
                        if (startPc != 0) {
                            continue;
                        }
                        newVariableAttribute.addEntry(startPc, codeLength, variableAttribute.nameIndex(i), variableAttribute.descriptorIndex(i), variableAttribute.index(i));
                    }
                    if (newVariableAttribute.tableLength() > 0) {
                        caAttributes.add(newVariableAttribute);
                    }
                    break;
                default:
            }
        }
    }

    public static boolean isEmptyCode(CodeAttribute codeAttribute) {
        switch (codeAttribute.getCodeLength()) {
            case 0:
                return true;
            case 1:
                return codeAttribute.getCode()[0] == (byte) Opcode.RETURN;
            case 2:
                byte[] code = codeAttribute.getCode();
                int b0 = code[0];
                switch (b0) {
                    case Opcode.ACONST_NULL:
                        return code[1] == (byte)Opcode.ARETURN;
                    case Opcode.ICONST_0:
                        return code[1] == (byte) Opcode.IRETURN;
                    case Opcode.FCONST_0:
                        return code[1] == (byte) Opcode.FRETURN;
                    case Opcode.LCONST_0:
                        return code[1] == (byte) Opcode.LRETURN;
                    case Opcode.DCONST_0:
                        return code[1] == (byte) Opcode.DRETURN;
                    default:
                        return false;
                }
            default:
                return false;
        }
    }
}