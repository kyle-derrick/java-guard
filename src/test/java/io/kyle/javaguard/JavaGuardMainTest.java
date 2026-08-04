package io.kyle.javaguard;

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class JavaGuardMainTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void noArgumentsReturnsNonzero() throws Exception {
        CapturedRun run = capture(new String[0]);

        Assert.assertNotEquals(0, run.exitCode);
        Assert.assertTrue(run.stdout.contains("usage: java-guard"));
        Assert.assertEquals("", run.stderr);
    }

    @Test
    public void helpReturnsZero() throws Exception {
        CapturedRun run = capture(new String[]{"--help"});

        Assert.assertEquals(0, run.exitCode);
        Assert.assertTrue(run.stdout.contains("usage: java-guard"));
        Assert.assertEquals("", run.stderr);
    }

    @Test
    public void unknownOptionReturnsNonzero() throws Exception {
        CapturedRun run = capture(new String[]{"--unknown-option"});

        Assert.assertNotEquals(0, run.exitCode);
        Assert.assertTrue(run.stdout.contains("usage: java-guard"));
        Assert.assertTrue(run.stderr.contains("ERROR: Invalid arguments:"));
    }

    @Test
    public void missingConfigReturnsNonzero() throws Exception {
        File missingConfig = new File(temporaryFolder.getRoot(), "missing.yml");

        CapturedRun run = capture(new String[]{"--config", missingConfig.getAbsolutePath()});

        Assert.assertNotEquals(0, run.exitCode);
        Assert.assertTrue(run.stderr.contains("Failed to read config file:"));
    }

    @Test
    public void emptyConfigReturnsNonzero() throws Exception {
        File config = temporaryFolder.newFile("empty.yml");

        CapturedRun run = capture(new String[]{"--config", config.getAbsolutePath()});

        Assert.assertNotEquals(0, run.exitCode);
        Assert.assertTrue(run.stderr.contains("Config file is empty:"));
    }

    @Test
    public void malformedYamlReturnsNonzero() throws Exception {
        File config = temporaryFolder.newFile("malformed.yml");
        Files.write(config.toPath(), "key: [unterminated\n".getBytes(StandardCharsets.UTF_8));

        CapturedRun run = capture(new String[]{"--config", config.getAbsolutePath()});

        Assert.assertNotEquals(0, run.exitCode);
        Assert.assertTrue(run.stderr.contains("Malformed config file:"));
    }

    @Test
    public void unsupportedModeReturnsNonzero() throws Exception {
        File config = temporaryFolder.newFile("config.yml");
        Files.write(config.toPath(), "{}\n".getBytes(StandardCharsets.UTF_8));

        CapturedRun run = capture(new String[]{
                "--config", config.getAbsolutePath(),
                "--mode", "unsupported"
        });

        Assert.assertNotEquals(0, run.exitCode);
        Assert.assertTrue(run.stderr.contains("not support mode: unsupported"));
    }

    @Test
    public void launcherOnlyInvokesGeneratorWithoutInputJar() throws Exception {
        File config = configWithPrivateKey();
        File output = temporaryFolder.newFolder("launcher-output");
        AtomicInteger calls = new AtomicInteger();

        CapturedRun run = capture(new String[]{
                "-l", "-c", config.getAbsolutePath(), "-o", output.getAbsolutePath()
        }, (generatedOutput, transformInfo) -> {
            calls.incrementAndGet();
            Assert.assertEquals(output.getAbsolutePath(), generatedOutput);
            Assert.assertNotNull(transformInfo.getKeyInfo());
            Assert.assertNotNull(transformInfo.getResourceKeyInfo());
            Assert.assertNotNull(transformInfo.getSignature());
        });

        Assert.assertEquals(0, run.exitCode);
        Assert.assertEquals("", run.stdout);
        Assert.assertEquals("", run.stderr);
        Assert.assertEquals(1, calls.get());
        Assert.assertEquals(0, output.list().length);
    }

    @Test
    public void noInputJarWithoutLauncherReturnsNonzero() throws Exception {
        File config = configWithPrivateKey();
        AtomicInteger calls = new AtomicInteger();

        CapturedRun run = capture(new String[]{"-c", config.getAbsolutePath()},
                (output, transformInfo) -> calls.incrementAndGet());

        Assert.assertEquals(1, run.exitCode);
        Assert.assertEquals("", run.stdout);
        Assert.assertTrue(run.stderr.contains("At least one input JAR is required"));
        Assert.assertEquals(0, calls.get());
    }

    @Test
    public void failedInPlaceSigningPreservesExistingFile() throws Exception {
        File config = configWithPrivateKey();
        File input = temporaryFolder.newFile("input.jar");
        byte[] original = "not a valid jar".getBytes(StandardCharsets.UTF_8);
        Files.write(input.toPath(), original);

        CapturedRun run = capture(new String[]{
                "-c", config.getAbsolutePath(),
                "-m", "signature",
                "-o", temporaryFolder.getRoot().getAbsolutePath(),
                input.getAbsolutePath()
        });

        Assert.assertEquals(1, run.exitCode);
        Assert.assertTrue(run.stderr.contains("ERROR: Transformation failed:"));
        Assert.assertArrayEquals(original, Files.readAllBytes(input.toPath()));
        File[] leftovers = temporaryFolder.getRoot().listFiles((dir, name) -> name.startsWith(".java-guard-"));
        Assert.assertNotNull(leftovers);
        Assert.assertEquals(0, leftovers.length);
    }

    private CapturedRun capture(String[] args) throws Exception {
        return capture(args, null);
    }

    private CapturedRun capture(String[] args, JavaGuardMain.LauncherGenerator launcherGenerator)
            throws Exception {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        try (PrintStream capturedOut = new PrintStream(stdout, true, "UTF-8");
             PrintStream capturedErr = new PrintStream(stderr, true, "UTF-8")) {
            System.setOut(capturedOut);
            System.setErr(capturedErr);
            int exitCode = launcherGenerator == null
                    ? JavaGuardMain.run(args)
                    : JavaGuardMain.run(args, launcherGenerator);
            return new CapturedRun(exitCode,
                    new String(stdout.toByteArray(), StandardCharsets.UTF_8),
                    new String(stderr.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    private File configWithPrivateKey() throws Exception {
        File privateKey = temporaryFolder.newFile("id-ed25519-" + System.nanoTime());
        try (PemWriter writer = new PemWriter(new FileWriter(privateKey))) {
            writer.writeObject(new PemObject("OPENSSH PRIVATE KEY", OpenSSHPrivateKeyUtil.encodePrivateKey(
                    new Ed25519PrivateKeyParameters(new SecureRandom()))));
        }
        File config = temporaryFolder.newFile("config-" + System.nanoTime() + ".yml");
        Files.write(config.toPath(), ("privateKey: '" + privateKey.getAbsolutePath().replace("'", "''") + "'\n"
                + "key: test-key\n").getBytes(StandardCharsets.UTF_8));
        return config;
    }

    private static final class CapturedRun {
        private final int exitCode;
        private final String stdout;
        private final String stderr;

        private CapturedRun(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }
}
