package io.kyle.javaguard.test;

import io.kyle.javaguard.bean.AppConfig;
import io.kyle.javaguard.bean.SignatureInfo;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil;
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;

public class SignatureInfoTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void requiresPrivateKey() {
        AppConfig config = new AppConfig();
        config.setPrivateKey(new File(temporaryFolder.getRoot(), "missing-key").getAbsolutePath());

        try {
            SignatureInfo.fromConfig(config);
            Assert.fail("expected private key failure");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("private key"));
        }
    }

    @Test
    public void derivesPublicKeyFromPrivateKey() throws Exception {
        Ed25519PrivateKeyParameters privateKey = new Ed25519PrivateKeyParameters(new SecureRandom());
        AppConfig config = configWithPrivateKey(privateKey);
        config.setPublicKey(new File(temporaryFolder.getRoot(), "missing-public-key").getAbsolutePath());

        try {
            SignatureInfo.fromConfig(config);
            Assert.fail("an explicitly configured missing public key must fail");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("public key"));
        }

        config.setPublicKey(null);
        SignatureInfo info = SignatureInfo.fromConfig(config);
        Assert.assertArrayEquals(privateKey.generatePublicKey().getEncoded(), info.getPublicKey().getEncoded());
    }

    @Test
    public void rejectsPublicKeyThatDoesNotMatchPrivateKey() throws Exception {
        Ed25519PrivateKeyParameters privateKey = new Ed25519PrivateKeyParameters(new SecureRandom());
        Ed25519PrivateKeyParameters otherKey = new Ed25519PrivateKeyParameters(new SecureRandom());
        AppConfig config = configWithPrivateKey(privateKey);
        File publicKeyFile = temporaryFolder.newFile("id_ed25519.pub");
        writePublicKey(publicKeyFile, otherKey);
        config.setPublicKey(publicKeyFile.getAbsolutePath());

        try {
            SignatureInfo.fromConfig(config);
            Assert.fail("expected mismatched key failure");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("does not match"));
        }
    }

    private AppConfig configWithPrivateKey(Ed25519PrivateKeyParameters privateKey) throws Exception {
        File privateKeyFile = temporaryFolder.newFile("private-" + System.nanoTime());
        try (PemWriter writer = new PemWriter(new FileWriter(privateKeyFile))) {
            writer.writeObject(new PemObject("OPENSSH PRIVATE KEY", OpenSSHPrivateKeyUtil.encodePrivateKey(privateKey)));
        }
        AppConfig config = new AppConfig();
        config.setPrivateKey(privateKeyFile.getAbsolutePath());
        return config;
    }

    private static void writePublicKey(File file, Ed25519PrivateKeyParameters privateKey) throws Exception {
        String encoded = Base64.encodeBase64String(OpenSSHPublicKeyUtil.encodePublicKey(privateKey.generatePublicKey()));
        Files.write(file.toPath(), ("ssh-ed25519 " + encoded + " test\n").getBytes(StandardCharsets.US_ASCII));
    }
}
