package io.kyle.javaguard;

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil;
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

public class JavaGuardMainTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void noArgumentsReturnsNonzero() {
        Assert.assertNotEquals(0, JavaGuardMain.run(new String[0]));
    }

    @Test
    public void helpReturnsZero() {
        Assert.assertEquals(0, JavaGuardMain.run(new String[]{"--help"}));
    }

    @Test
    public void unknownOptionReturnsNonzero() {
        Assert.assertNotEquals(0, JavaGuardMain.run(new String[]{"--unknown-option"}));
    }

    @Test
    public void missingConfigReturnsNonzero() {
        File missingConfig = new File(temporaryFolder.getRoot(), "missing.yml");

        Assert.assertNotEquals(0, JavaGuardMain.run(new String[]{"--config", missingConfig.getAbsolutePath()}));
    }

    @Test
    public void emptyConfigReturnsNonzero() throws Exception {
        File config = temporaryFolder.newFile("empty.yml");

        Assert.assertNotEquals(0, JavaGuardMain.run(new String[]{"--config", config.getAbsolutePath()}));
    }

    @Test
    public void malformedYamlReturnsNonzero() throws Exception {
        File config = temporaryFolder.newFile("malformed.yml");
        Files.write(config.toPath(), "key: [unterminated\n".getBytes(StandardCharsets.UTF_8));

        Assert.assertNotEquals(0, JavaGuardMain.run(new String[]{"--config", config.getAbsolutePath()}));
    }

    @Test
    public void unsupportedModeReturnsNonzero() throws Exception {
        File config = temporaryFolder.newFile("config.yml");
        Files.write(config.toPath(), "{}\n".getBytes(StandardCharsets.UTF_8));

        Assert.assertNotEquals(0, JavaGuardMain.run(new String[]{
                "--config", config.getAbsolutePath(),
                "--mode", "unsupported"
        }));
    }

    @Test
    public void failedInPlaceSigningPreservesExistingFile() throws Exception {
        File privateKey = temporaryFolder.newFile("id_ed25519");
        try (PemWriter writer = new PemWriter(new FileWriter(privateKey))) {
            writer.writeObject(new PemObject("OPENSSH PRIVATE KEY", OpenSSHPrivateKeyUtil.encodePrivateKey(
                    new Ed25519PrivateKeyParameters(new SecureRandom()))));
        }
        File config = temporaryFolder.newFile("config.yml");
        Files.write(config.toPath(), ("privateKey: '" + privateKey.getAbsolutePath().replace("'", "''") + "'\n"
                + "key: test-key\n").getBytes(StandardCharsets.UTF_8));
        File input = temporaryFolder.newFile("input.jar");
        byte[] original = "not a valid jar".getBytes(StandardCharsets.UTF_8);
        Files.write(input.toPath(), original);

        int result = JavaGuardMain.run(new String[]{
                "-c", config.getAbsolutePath(),
                "-m", "signature",
                "-o", temporaryFolder.getRoot().getAbsolutePath(),
                input.getAbsolutePath()
        });

        Assert.assertEquals(1, result);
        Assert.assertArrayEquals(original, Files.readAllBytes(input.toPath()));
        File[] leftovers = temporaryFolder.getRoot().listFiles((dir, name) -> name.startsWith(".java-guard-"));
        Assert.assertNotNull(leftovers);
        Assert.assertEquals(0, leftovers.length);
    }
}
