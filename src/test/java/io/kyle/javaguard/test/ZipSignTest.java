package io.kyle.javaguard.test;

import io.kyle.javaguard.bean.SignatureInfo;
import io.kyle.javaguard.util.ZipSignUtils;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemReader;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.security.Security;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/29 15:26
 */
@Ignore
public class ZipSignTest {
    private static final String zipPath = "out/tmp/jar-test-1.0-SNAPSHOT-jar-with-dependencies.jar";
    private static final String zipSignPath = "out/tmp/jar-test-1.0-SNAPSHOT-jar-with-dependencies-sign.jar";
    static {
        Security.addProvider(new BouncyCastleProvider());
    }
    @Test
    public void testSign() throws Exception {
        File zipFile = new File(zipPath);
        File zipSignFile = new File(zipSignPath);
        if (zipSignFile.exists()) {
            FileUtils.forceDelete(zipSignFile);
        }
        FileUtils.copyFile(zipFile, zipSignFile);
        SignatureInfo signatureInfo = new SignatureInfo();
        try (PemReader privateKeyReader = new PemReader(new FileReader("out/private.pem"));
             PemReader publicKeyReader = new PemReader(new FileReader("out/public.pem"))) {
            signatureInfo.setPrivateKey(privateKeyReader.readPemObject().getContent());
            signatureInfo.setPublicKey(publicKeyReader.readPemObject().getContent());
        } catch (Exception e) {
            e.printStackTrace();
        }
        ZipSignUtils.sign(zipSignFile, signatureInfo.getSignSignature());

        ZipSignUtils.verify(zipSignFile, signatureInfo.newVerifySignature());
    }
}
