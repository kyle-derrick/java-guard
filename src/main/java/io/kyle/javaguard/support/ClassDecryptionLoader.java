package io.kyle.javaguard.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/10 17:17
 */
public class ClassDecryptionLoader extends ClassLoader {
    public static Object decryption(byte[] customClasses, byte[] javassist) {
        ClassDecryptionLoader factory = new ClassDecryptionLoader();
        try {
            factory.importJavassist(javassist);
            return factory.defineCustomClasses(customClasses);
        } catch (Exception e) {
            throw new Error("Failed to import load decryption module", e);
        }
    }

    public ClassDecryptionLoader() {
        super(null);
    }

    private Class<?> defineCustomClasses(byte[] customClasses) {
        ByteBuffer buffer = ByteBuffer.wrap(customClasses);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int len = buffer.getInt();
        byte[] bytes = new byte[len];
        buffer.get(bytes);
        Class<?> result = defineClassWithoutName(bytes);
        while (buffer.position() < buffer.capacity()) {
            len = buffer.getInt();
            bytes = new byte[len];
            buffer.get(bytes);
            String name = new String(bytes, StandardCharsets.UTF_8);
            len = buffer.getInt();
            bytes = new byte[len];
            buffer.get(bytes);
            defineClass(name, bytes);
        }
        return result;
    }

    private void importJavassist(byte[] javassist) throws IOException {
        JarInputStream stream = new JarInputStream(new ByteArrayInputStream(javassist));
        ZipEntry nextEntry;
        while ((nextEntry = stream.getNextEntry()) != null) {
            String name = nextEntry.getName();
            if (!name.endsWith(".class")) {
                continue;
            }
            int size = (int) nextEntry.getSize();
            byte[] buff = new byte[size];
            stream.read(buff);
            stream.closeEntry();
            defineClass(name.replace('/', '.'), buff);
        }
    }

    private Class<?> defineClassWithoutName(byte[] b) {
        return defineClass(null, b, 0, b.length);
    }
    private void defineClass(String name, byte[] b) {
        defineClass(name, b, 0, b.length);
    }
}
