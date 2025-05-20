package io.kyle.javaguard.test;

import io.kyle.javaguard.support.AesGcmResourceOutputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2025/5/16 16:33
 */
@Ignore
public class ResourceStreamTest {
//    private static final String path = "E:\\data\\down\\sys\\ubuntu-24.04-desktop-amd64.iso";
    private static final String path = "D:\\data\\down\\baidu\\VMware-VMRC-12.0.0-17287072.zip";

    @Test
    public void test() throws Exception {
        byte[] iv = new byte[12];
        byte[] key = new byte[32];
        int bufSize = 4*1024*1024;
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), gcmSpec);
        try (InputStream in = new BufferedInputStream(Files.newInputStream(Paths.get(path)), bufSize);
             OutputStream outputStream = new AesGcmResourceOutputStream(new BufferedOutputStream(Files.newOutputStream(Paths.get(path + "_en.iso")), bufSize), key, iv, true)) {
            IOUtils.copyLarge(in, outputStream);
        }

        cipher = Cipher.getInstance("AES/GCM/NoPadding");
        gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), gcmSpec);
        try (InputStream in = new BufferedInputStream(Files.newInputStream(Paths.get(path + "_en.iso")), bufSize);
             OutputStream outputStream = new AesGcmResourceOutputStream(new BufferedOutputStream(Files.newOutputStream(Paths.get(path + "_de.iso")), bufSize), key, iv, false)) {
            IOUtils.copyLarge(in, outputStream);
        }

    }
}
