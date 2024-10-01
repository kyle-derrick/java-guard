package io.kyle.javaguard.transform.encryption;

import io.kyle.javaguard.bean.EncryptInfo;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/9/30 14:17
 */
public class SimpleEncryption extends AbstractEncryption {

    public SimpleEncryption(EncryptInfo encryptInfo) {
        super(encryptInfo);
    }

    @Override
    public boolean isSupport(String name) {
        return true;
    }

    @Override
    public void encrypt(InputStream in, OutputStream out) throws IOException {
        // todo
        IOUtils.copy(in, out);
    }

    @Override
    public void decrypt(InputStream in, OutputStream out) throws IOException {
        // todo
        IOUtils.copy(in, out);
    }
}