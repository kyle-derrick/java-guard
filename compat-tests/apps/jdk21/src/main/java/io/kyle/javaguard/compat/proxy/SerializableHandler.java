package io.kyle.javaguard.compat.proxy;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
public class SerializableHandler implements InvocationHandler, Serializable {
    private static final long serialVersionUID = 1L;
    public Object invoke(Object proxy, Method method, Object[] args) { return "proxied"; }
}
