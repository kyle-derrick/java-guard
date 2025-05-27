package io.kyle.javaguard.test;

import io.kyle.javaguard.bean.KeyInfo;
import io.kyle.javaguard.support.StandardResourceInputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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
        byte[] key = new byte[32];
        int bufSize = 4*1024*1024;
        KeyInfo keyInfo = new KeyInfo(key);
        try (InputStream in = new StandardResourceInputStream(new BufferedInputStream(Files.newInputStream(Paths.get(path)), bufSize), keyInfo, true);
             OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(Paths.get(path + "_en.iso")), bufSize)) {
            IOUtils.copyLarge(in, outputStream);
        }

        try (InputStream in = new StandardResourceInputStream(new BufferedInputStream(Files.newInputStream(Paths.get(path + "_en.iso")), bufSize), keyInfo, false);
             OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(Paths.get(path + "_de.iso")), bufSize)) {
            IOUtils.copyLarge(in, outputStream);
        }

    }
}
