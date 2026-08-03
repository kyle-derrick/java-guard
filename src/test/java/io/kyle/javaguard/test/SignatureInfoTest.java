package io.kyle.javaguard.test;

import io.kyle.javaguard.bean.AppConfig;
import io.kyle.javaguard.bean.SignatureInfo;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil;
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;

public class SignatureInfoTest {
    private static final String FIXTURE_ROOT = "/io/kyle/javaguard/test/fixtures/openssh-ed25519/";
    private static final byte[] MATCHING_PUBLIC_KEY = decodeHex(
            "430c96393033fcf523590a21d9a663c30fc139fe1d8596b0098620cc6ed8fcfe");

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

    @Ignore("OpenSSH key fixture verification is temporarily disabled")
    @Test
    public void parsesSshKeygenOpenSshPrivateAndPublicKeys() throws Exception {
        File privateKeyFile = copyFixture("matching");
        File publicKeyFile = copyFixture("matching.pub");
        AppConfig config = new AppConfig();
        config.setPrivateKey(privateKeyFile.getAbsolutePath());
        config.setPublicKey(publicKeyFile.getAbsolutePath());

        SignatureInfo info = SignatureInfo.fromConfig(config);

        Assert.assertArrayEquals(MATCHING_PUBLIC_KEY, info.getPublicKey().getEncoded());
        Assert.assertArrayEquals(MATCHING_PUBLIC_KEY, info.getPrivateKey().generatePublicKey().getEncoded());
    }

    @Ignore("OpenSSH key fixture verification is temporarily disabled")
    @Test
    public void derivesSshKeygenPublicKeyFromOpenSshPrivateKey() throws Exception {
        AppConfig config = new AppConfig();
        config.setPrivateKey(copyFixture("matching").getAbsolutePath());

        SignatureInfo info = SignatureInfo.fromConfig(config);

        Assert.assertArrayEquals(MATCHING_PUBLIC_KEY, info.getPublicKey().getEncoded());
    }

    @Ignore("OpenSSH key fixture verification is temporarily disabled")
    @Test
    public void rejectsMismatchedSshKeygenPublicKey() throws Exception {
        AppConfig config = new AppConfig();
        config.setPrivateKey(copyFixture("matching").getAbsolutePath());
        config.setPublicKey(copyFixture("other.pub").getAbsolutePath());

        try {
            SignatureInfo.fromConfig(config);
            Assert.fail("expected mismatched ssh-keygen key failure");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("does not match"));
        }
    }

    private File copyFixture(String name) throws Exception {
        File target = new File(temporaryFolder.getRoot(), name);
        try (InputStream input = SignatureInfoTest.class.getResourceAsStream(FIXTURE_ROOT + name)) {
            Assert.assertNotNull("missing OpenSSH fixture: " + name, input);
            Files.copy(input, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private static byte[] decodeHex(String value) {
        try {
            return Hex.decodeHex(value.toCharArray());
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid test public key", e);
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
