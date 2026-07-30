package io.kyle.javaguard.test;

import io.kyle.javaguard.bean.AppConfig;
import io.kyle.javaguard.bean.SignatureInfo;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil;
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
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

    @Test
    public void parsesSshKeygenOpenSshPrivateAndPublicKeys() throws Exception {
        GeneratedKeyPair keyPair = generateOpenSshKeyPair("matching");
        AppConfig config = new AppConfig();
        config.setPrivateKey(keyPair.privateKeyFile.getAbsolutePath());
        config.setPublicKey(keyPair.publicKeyFile.getAbsolutePath());

        SignatureInfo info = SignatureInfo.fromConfig(config);

        Assert.assertArrayEquals(keyPair.publicKey.getEncoded(), info.getPublicKey().getEncoded());
        Assert.assertArrayEquals(keyPair.publicKey.getEncoded(), info.getPrivateKey().generatePublicKey().getEncoded());
    }

    @Test
    public void derivesSshKeygenPublicKeyFromOpenSshPrivateKey() throws Exception {
        GeneratedKeyPair keyPair = generateOpenSshKeyPair("derived");
        AppConfig config = new AppConfig();
        config.setPrivateKey(keyPair.privateKeyFile.getAbsolutePath());

        SignatureInfo info = SignatureInfo.fromConfig(config);

        Assert.assertArrayEquals(keyPair.publicKey.getEncoded(), info.getPublicKey().getEncoded());
    }

    @Test
    public void rejectsMismatchedSshKeygenPublicKey() throws Exception {
        GeneratedKeyPair privateKeyPair = generateOpenSshKeyPair("private");
        GeneratedKeyPair otherKeyPair = generateOpenSshKeyPair("other");
        AppConfig config = new AppConfig();
        config.setPrivateKey(privateKeyPair.privateKeyFile.getAbsolutePath());
        config.setPublicKey(otherKeyPair.publicKeyFile.getAbsolutePath());

        try {
            SignatureInfo.fromConfig(config);
            Assert.fail("expected mismatched ssh-keygen key failure");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("does not match"));
        }
    }

    private GeneratedKeyPair generateOpenSshKeyPair(String name) throws Exception {
        File privateKeyFile = new File(temporaryFolder.getRoot(), "id_ed25519_" + name);
        Process process = new ProcessBuilder(
                "ssh-keygen", "-q", "-t", "ed25519", "-N", "", "-C", "SignatureInfoTest", "-f",
                privateKeyFile.getAbsolutePath())
                .redirectErrorStream(true)
                .start();
        byte[] output = readProcessOutput(process);
        int exitCode = process.waitFor();
        Assert.assertEquals("ssh-keygen failed: " + new String(output, StandardCharsets.UTF_8), 0, exitCode);

        File publicKeyFile = new File(privateKeyFile.getAbsolutePath() + ".pub");
        Assert.assertTrue("ssh-keygen did not create a private key", privateKeyFile.isFile());
        Assert.assertTrue("ssh-keygen did not create a public key", publicKeyFile.isFile());
        return new GeneratedKeyPair(privateKeyFile, publicKeyFile, parseOpenSshPublicKey(publicKeyFile));
    }

    private static byte[] readProcessOutput(Process process) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        try (InputStream input = process.getInputStream()) {
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }
        return output.toByteArray();
    }

    private static Ed25519PublicKeyParameters parseOpenSshPublicKey(File publicKeyFile) throws Exception {
        String content = new String(Files.readAllBytes(publicKeyFile.toPath()), StandardCharsets.US_ASCII).trim();
        String[] fields = content.split("\\s+");
        Assert.assertEquals("ssh-ed25519", fields[0]);
        AsymmetricKeyParameter key = OpenSSHPublicKeyUtil.parsePublicKey(Base64.decodeBase64(fields[1]));
        Assert.assertTrue(key instanceof Ed25519PublicKeyParameters);
        return (Ed25519PublicKeyParameters) key;
    }

    private static final class GeneratedKeyPair {
        private final File privateKeyFile;
        private final File publicKeyFile;
        private final Ed25519PublicKeyParameters publicKey;

        private GeneratedKeyPair(File privateKeyFile, File publicKeyFile, Ed25519PublicKeyParameters publicKey) {
            this.privateKeyFile = privateKeyFile;
            this.publicKeyFile = publicKeyFile;
            this.publicKey = publicKey;
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
