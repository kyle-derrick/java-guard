package io.kyle.javaguard.util;

import io.kyle.javaguard.exception.TransformException;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

public class LambdaUtils {
    public static <T, R> SerializedLambda getInfo(SFunction<T, R> func) throws TransformException {
        try {
            Method writeReplace = func.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            return (SerializedLambda) writeReplace.invoke(func);
        } catch (Exception e) {
            throw new TransformException(e);
        }
    }

    public static <T> String getMethodName(SFunction<T, ?> func) throws TransformException {
        return getInfo(func).getImplMethodName();
    }

    public static <T> String getMethodDescriptor(SFunction<T, ?> func) throws TransformException {
        return getInfo(func).getImplMethodSignature();
    }

    public static <T> String getClassName(SFunction<T, ?> func) throws TransformException {
        return getInfo(func).getImplClass();
    }
}
