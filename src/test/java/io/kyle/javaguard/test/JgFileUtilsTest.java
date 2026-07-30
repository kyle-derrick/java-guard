package io.kyle.javaguard.util;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

public class JgFileUtilsTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void zipJavaDirectoryReplacesLauncherAndRetainsOriginal() throws Exception {
        Fixture fixture = fixture("java.exe");
        File archive = temporaryFolder.newFile("jdk.zip");

        JgFileUtils.zipJavaDirectory(fixture.javaHome, archive, fixture.launcher);

        assertPackagedJava(readZip(archive), fixture.rootName, fixture.javaName);
    }

    @Test
    public void tarGzJavaDirectoryReplacesLauncherAndRetainsOriginal() throws Exception {
        Fixture fixture = fixture("java");
        File archive = temporaryFolder.newFile("jdk.tar.gz");

        JgFileUtils.tarGzJavaDirectory(fixture.javaHome, archive, fixture.launcher);

        assertPackagedJava(readTarGz(archive), fixture.rootName, fixture.javaName);
    }

    @Test
    public void javaDirectoryPackagingFailsWhenBinJavaIsMissing() throws Exception {
        File javaHome = javaHomeWithoutLauncher();
        File launcher = launcher();

        assertMissingJavaFails(javaHome, temporaryFolder.newFile("missing.zip"), launcher, true);
        assertMissingJavaFails(javaHome, temporaryFolder.newFile("missing.tar.gz"), launcher, false);
    }

    @Test
    public void javaArchivePackagingFailsWhenBinJavaIsMissing() throws Exception {
        File javaHome = javaHomeWithoutLauncher();
        File launcher = launcher();
        File sourceZip = temporaryFolder.newFile("source.zip");
        File sourceTarGz = temporaryFolder.newFile("source.tar.gz");
        JgFileUtils.zip(javaHome, sourceZip);
        JgFileUtils.tarGz(javaHome, sourceTarGz);

        assertMissingJavaArchiveFails(sourceZip, temporaryFolder.newFile("result.zip"), launcher, true);
        assertMissingJavaArchiveFails(sourceTarGz, temporaryFolder.newFile("result.tar.gz"), launcher, false);
    }

    private File javaHomeWithoutLauncher() throws Exception {
        File javaHome = temporaryFolder.newFolder("jdk-without-java");
        Assert.assertTrue(new File(javaHome, "bin").mkdir());
        return javaHome;
    }

    private File launcher() throws Exception {
        File launcher = temporaryFolder.newFile("java");
        Files.write(launcher.toPath(), bytes("replacement-launcher"));
        return launcher;
    }

    private static void assertMissingJavaFails(File source, File target, File launcher, boolean zip) throws Exception {
        byte[] previousTarget = bytes("previous-directory-package");
        Files.write(target.toPath(), previousTarget);
        try {
            if (zip) {
                JgFileUtils.zipJavaDirectory(source, target, launcher);
            } else {
                JgFileUtils.tarGzJavaDirectory(source, target, launcher);
            }
            Assert.fail("expected missing bin/java failure");
        } catch (java.io.IOException expected) {
            Assert.assertTrue(expected.getMessage().contains("bin/java"));
        }
        Assert.assertArrayEquals("failed package must preserve the prior target",
                previousTarget, Files.readAllBytes(target.toPath()));
    }

    private static void assertMissingJavaArchiveFails(File source, File target, File launcher, boolean zip) throws Exception {
        byte[] previousTarget = bytes("previous-archive-package");
        Files.write(target.toPath(), previousTarget);
        try {
            if (zip) {
                JgFileUtils.zipJava(source, target, launcher);
            } else {
                JgFileUtils.tarGzJava(source, target, launcher);
            }
            Assert.fail("expected missing bin/java failure");
        } catch (java.io.IOException expected) {
            Assert.assertTrue(expected.getMessage().contains("bin/java"));
        }
        Assert.assertArrayEquals("failed package must preserve the prior target",
                previousTarget, Files.readAllBytes(target.toPath()));
    }

    private Fixture fixture(String javaName) throws Exception {
        File javaHome = temporaryFolder.newFolder("portable-jdk");
        File bin = new File(javaHome, "bin");
        File lib = new File(javaHome, "lib");
        Assert.assertTrue(bin.mkdir());
        Assert.assertTrue(lib.mkdir());
        Files.write(new File(bin, javaName).toPath(), bytes("original-java"));
        Files.write(new File(lib, "runtime.txt").toPath(), bytes("runtime-data"));
        File launcher = temporaryFolder.newFile(javaName);
        Files.write(launcher.toPath(), bytes("replacement-launcher"));
        return new Fixture(javaHome, launcher, javaHome.getName(), javaName);
    }

    private static void assertPackagedJava(Map<String, EntryData> entries,
                                            String rootName, String javaName) {
        String prefix = rootName + "/";
        String extension = javaName.endsWith(".exe") ? ".exe" : "";
        assertFile(entries, prefix + "bin/" + javaName, "replacement-launcher");
        assertFile(entries, prefix + "bin/java_ori" + extension, "original-java");
        assertFile(entries, prefix + "lib/runtime.txt", "runtime-data");
        Assert.assertTrue("root directory entry missing", entries.get(rootName) != null
                && entries.get(rootName).directory);
        Assert.assertTrue("bin directory entry missing", entries.get(prefix + "bin") != null
                && entries.get(prefix + "bin").directory);
    }

    private static void assertFile(Map<String, EntryData> entries, String name, String expected) {
        EntryData entry = entries.get(name);
        Assert.assertNotNull("archive entry missing: " + name, entry);
        Assert.assertFalse("expected file: " + name, entry.directory);
        Assert.assertArrayEquals(bytes(expected), entry.content);
    }

    private static Map<String, EntryData> readZip(File archive) throws Exception {
        Map<String, EntryData> entries = new LinkedHashMap<String, EntryData>();
        try (ZipArchiveInputStream input = new ZipArchiveInputStream(Files.newInputStream(archive.toPath()))) {
            ZipArchiveEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                assertPortableEntryName(entry.getName());
                entries.put(trimTrailingSlash(entry.getName()),
                        new EntryData(entry.isDirectory(), entry.isDirectory() ? new byte[0] : read(input)));
            }
        }
        return entries;
    }

    private static Map<String, EntryData> readTarGz(File archive) throws Exception {
        Map<String, EntryData> entries = new LinkedHashMap<String, EntryData>();
        try (TarArchiveInputStream input = new TarArchiveInputStream(
                new GzipCompressorInputStream(Files.newInputStream(archive.toPath())))) {
            TarArchiveEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                assertPortableEntryName(entry.getName());
                entries.put(trimTrailingSlash(entry.getName()),
                        new EntryData(entry.isDirectory(), entry.isDirectory() ? new byte[0] : read(input)));
            }
        }
        return entries;
    }

    private static byte[] read(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        IOUtils.copy(input, output);
        return output.toByteArray();
    }

    private static void assertPortableEntryName(String name) {
        Assert.assertFalse("archive entry must use raw '/' separators: " + name, name.contains("\\"));
    }

    private static String trimTrailingSlash(String name) {
        while (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        return name;
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static final class Fixture {
        private final File javaHome;
        private final File launcher;
        private final String rootName;
        private final String javaName;

        private Fixture(File javaHome, File launcher, String rootName, String javaName) {
            this.javaHome = javaHome;
            this.launcher = launcher;
            this.rootName = rootName;
            this.javaName = javaName;
        }
    }

    private static final class EntryData {
        private final boolean directory;
        private final byte[] content;

        private EntryData(boolean directory, byte[] content) {
            this.directory = directory;
            this.content = content;
        }
    }
}
