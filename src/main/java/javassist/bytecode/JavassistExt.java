package javassist.bytecode;

import java.util.Set;

public class JavassistExt {

    public static int methodNameIndex(MethodInfo methodInfo) {
        return methodInfo.name;
    }
    public static int methodDescriptorIndex(MethodInfo methodInfo) {
        return methodInfo.descriptor;
    }

    public static int fieldNameIndex(FieldInfo fieldInfo) {
        return fieldInfo.name;
    }
    public static int fieldDescriptorIndex(FieldInfo fieldInfo) {
        return fieldInfo.descriptor;
    }
    public static int attributeNameIndex(AttributeInfo attributeInfo) {
        return attributeInfo.name;
    }
    public static int signatureAttributeIndex(SignatureAttribute signatureAttribute) {
        return ByteArray.readU16bit(signatureAttribute.get(), 0);
    }
    public static int sourceFileAttributeIndex(SourceFileAttribute signatureAttribute) {
        return ByteArray.readU16bit(signatureAttribute.get(), 0);
    }

    public static void setConst(IntegerInfo info, int value) {
        info.value = value;
    }

    public static void setConst(FloatInfo info, float value) {
        info.value = value;
    }

    public static void setConst(LongInfo info, long value) {
        info.value = value;
    }

    public static void setConst(DoubleInfo info, double value) {
        info.value = value;
    }

    public static void setConst(Utf8Info info, String value) {
        info.string = value;
    }

    public static void retainConst(ConstPool constPool, int index, Set<Integer> retainConst) {
        if (!retainConst.add(index)) {
            return;
        }
        ConstInfo item = constPool.getItem(index);
        if (item == null) {
            return;
        }
        switch (item.getTag()) {
            case ConstPool.CONST_Class:
                ClassInfo classInfo = (ClassInfo) item;
                retainConst(constPool, classInfo.name, retainConst);
                break;
            case ConstPool.CONST_Fieldref:
            case ConstPool.CONST_Methodref:
            case ConstPool.CONST_InterfaceMethodref:
                MemberrefInfo memberrefInfo = (MemberrefInfo) item;
                retainConst(constPool, memberrefInfo.classIndex, retainConst);
                retainConst(constPool, memberrefInfo.nameAndTypeIndex, retainConst);
                break;
            case ConstPool.CONST_String:
                StringInfo stringInfo = (StringInfo) item;
                retainConst(constPool, stringInfo.string, retainConst);
                break;
            case ConstPool.CONST_NameAndType:
                NameAndTypeInfo nameAndTypeInfo = (NameAndTypeInfo) item;
                retainConst(constPool, nameAndTypeInfo.memberName, retainConst);
                retainConst(constPool, nameAndTypeInfo.typeDescriptor, retainConst);
                break;
            case ConstPool.CONST_MethodHandle:
                MethodHandleInfo methodHandleInfo = (MethodHandleInfo) item;
                retainConst(constPool, methodHandleInfo.refIndex, retainConst);
                // methodHandleInfo.refKind 为引用类型，在ConstPool中的常量里有列举
                break;
            case ConstPool.CONST_MethodType:
                MethodTypeInfo methodTypeInfo = (MethodTypeInfo) item;
                retainConst(constPool, methodTypeInfo.descriptor, retainConst);
                break;
            case ConstPool.CONST_Dynamic:
                DynamicInfo dynamicInfo = (DynamicInfo) item;
                retainConst(constPool, dynamicInfo.nameAndType, retainConst);
                // invokeDynamicInfo.bootstrap 指向 class attribute中的BootstrapMethods中的索引
                break;
//            case ConstPool.CONST_DynamicCallSite:
            case ConstPool.CONST_InvokeDynamic:
                InvokeDynamicInfo invokeDynamicInfo = (InvokeDynamicInfo) item;
                retainConst(constPool, invokeDynamicInfo.nameAndType, retainConst);
                // invokeDynamicInfo.bootstrap 指向 class attribute中的BootstrapMethods中的索引
                break;
            case ConstPool.CONST_Module:
                ModuleInfo moduleInfo = (ModuleInfo) item;
                retainConst(constPool, moduleInfo.name, retainConst);
                break;
            case ConstPool.CONST_Package:
                PackageInfo packageInfo = (PackageInfo) item;
                retainConst(constPool, packageInfo.name, retainConst);
                break;
            default:
        }
    }

    public static void retainClassInfoConst(ConstPool constPool, Set<Integer> retainConst) {
        for (int i = 0; i < constPool.getSize(); i++) {
            ConstInfo item = constPool.getItem(i);
            if (item == null) {
                continue;
            }
            switch (item.getTag()) {
                case ConstPool.CONST_Class:
                case ConstPool.CONST_Fieldref:
                case ConstPool.CONST_Methodref:
                case ConstPool.CONST_InterfaceMethodref:
                case ConstPool.CONST_NameAndType:
                case ConstPool.CONST_MethodHandle:
                case ConstPool.CONST_MethodType:
                case ConstPool.CONST_Dynamic:
    //            case ConstPool.CONST_DynamicCallSite:
                case ConstPool.CONST_InvokeDynamic:
                case ConstPool.CONST_Module:
                case ConstPool.CONST_Package:
                    retainConst(constPool, i, retainConst);
                    break;
                default:
            }
        }
    }

//    public static void retainConst(ConstPool constPool, int index, Set<Integer> retainConst) {
//        ConstInfo item = constPool.getItem(index);
//        switch (constPool.getTag(index)) {
//            case ConstPool.CONST_Class:
//                ClassInfo classInfo = (ClassInfo) item;
//                retainConst.add(classInfo.name);
//                retainConst(constPool, classInfo.name, retainConst);
//                break;
//            case ConstPool.CONST_Fieldref:
//                FieldrefInfo fieldrefInfo = (FieldrefInfo) item;
//                retainConst.add(fieldrefInfo.classIndex);
//                retainConst(constPool, fieldrefInfo.classIndex, retainConst);
//                retainConst.add(fieldrefInfo.nameAndTypeIndex);
//                retainConst(constPool, fieldrefInfo.nameAndTypeIndex, retainConst);
//                break;
//            case ConstPool.CONST_Methodref:
//                MethodrefInfo methodrefInfo = (MethodrefInfo) item;
//                break;
//            case ConstPool.CONST_InterfaceMethodref:
//                InterfaceMethodrefInfo interfaceMethodrefInfo = (InterfaceMethodrefInfo) item;
//            case ConstPool.CONST_String:
//                StringInfo stringInfo = (StringInfo) item;
//                break;
//            case ConstPool.CONST_Integer:
//                IntegerInfo integerInfo = (IntegerInfo) item;
//                break;
//            case ConstPool.CONST_Float:
//                FloatInfo floatInfo = (FloatInfo) item;
//                break;
//            case ConstPool.CONST_Long:
//                LongInfo longInfo = (LongInfo) item;
//                break;
//            case ConstPool.CONST_Double:
//                DoubleInfo doubleInfo = (DoubleInfo) item;
//                break;
//            case ConstPool.CONST_NameAndType:
//                NameAndTypeInfo nameAndTypeInfo = (NameAndTypeInfo) item;
//                break;
//            case ConstPool.CONST_Utf8:
//                Utf8Info utf8Info = (Utf8Info) item;
//                break;
//            case ConstPool.CONST_MethodHandle:
//                MethodHandleInfo methodHandleInfo = (MethodHandleInfo) item;
//                break;
//            case ConstPool.CONST_MethodType:
//                MethodTypeInfo methodTypeInfo = (MethodTypeInfo) item;
//                break;
//            case ConstPool.CONST_Dynamic:
//                DynamicInfo dynamicInfo = (DynamicInfo) item;
//                break;
////            case ConstPool.CONST_DynamicCallSite:
//            case ConstPool.CONST_InvokeDynamic:
//                InvokeDynamicInfo invokeDynamicInfo = (InvokeDynamicInfo) item;
//                break;
//            case ConstPool.CONST_Module:
//                ModuleInfo moduleInfo = (ModuleInfo) item;
//                break;
//            case ConstPool.CONST_Package:
//                PackageInfo packageInfo = (PackageInfo) item;
//                break;
//        }
//    }
}
