package io.kyle.javaguard.transform.encryption;

import io.kyle.javaguard.bean.EncryptInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/9/30 16:41
 */
public interface Encryption {
    EncryptionCreator[] encryption = {
            ClassEncryption::new,
            JarEncryption::new,
            SimpleEncryption::new,
    };

    boolean isSupport(String name);
    void encrypt(final InputStream in, final OutputStream out) throws IOException;
    void decrypt(final InputStream in, final OutputStream out) throws IOException;

    interface EncryptionCreator {
        Encryption create(EncryptInfo encryptInfo);
    }
}
