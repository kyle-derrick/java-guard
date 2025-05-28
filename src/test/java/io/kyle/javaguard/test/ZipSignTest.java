package io.kyle.javaguard.test;

import io.kyle.javaguard.bean.AppConfig;
import io.kyle.javaguard.bean.SignatureInfo;
import io.kyle.javaguard.util.ZipSignUtils;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.security.Security;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/29 15:26
 */
@Ignore
public class ZipSignTest {
    private static final String zipPath = "out/sign/antlr-4.13.2-complete.jar";
    private static final String zipSignPath = "out/sign/antlr-4.13.2-complete.sign.jar";
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
        AppConfig appConfig = new AppConfig();
        appConfig.setPrivateKey("out\\id_ed25519");
        appConfig.setPublicKey("out\\id_ed25519.pub");
        SignatureInfo signatureInfo = SignatureInfo.fromConfig(appConfig);
        if (signatureInfo == null) {
            return;
        }

        ZipSignUtils.sign(zipSignFile, signatureInfo.getSignSignature());

        ZipSignUtils.verify(zipSignFile, signatureInfo.newVerifySigner());
    }

    @Test
    public void verify() throws Exception {
        String path = "out/tmp/antlr-4.13.2-complete.jar";
        File zipSignFile = new File(path);
        AppConfig appConfig = new AppConfig();
        appConfig.setPrivateKey("out\\id_ed25519");
        appConfig.setPublicKey("out\\id_ed25519.pub");
        SignatureInfo signatureInfo = SignatureInfo.fromConfig(appConfig);
        if (signatureInfo == null) {
            return;
        }

        ZipSignUtils.verify(zipSignFile, signatureInfo.newVerifySigner());
    }
}
