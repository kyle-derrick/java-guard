package io.kyle.javaguard.transform.encryption;

import io.kyle.javaguard.bean.EncryptInfo;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/9/30 14:17
 */
public class JarEncryption extends AbstractEncryption {

    public JarEncryption(EncryptInfo encryptInfo) {
        super(encryptInfo);
    }

    @Override
    public boolean isSupport(String name) {
        return StringUtils.endsWith(name, ".jar");
    }

    @Override
    public void encrypt(InputStream in, OutputStream out) throws IOException {

    }

    @Override
    public void decrypt(InputStream in, OutputStream out) throws IOException {
        
    }
}