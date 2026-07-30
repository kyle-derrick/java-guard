package io.kyle.javaguard.support;

import io.kyle.javaguard.exception.TransformException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class LauncherCodeGeneratorTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void executeCommandRejectsNonzeroExit() throws Exception {
        File javaExecutable = new File(new File(System.getProperty("java.home"), "bin"),
                isWindows() ? "java.exe" : "java");

        try {
            LauncherCodeGenerator.executeCommand(temporaryFolder.getRoot(),
                    new String[]{javaExecutable.getAbsolutePath(), "-option-that-does-not-exist"});
            Assert.fail("Expected command failure");
        } catch (TransformException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("退出码"));
            Assert.assertTrue(e.getMessage(), e.getMessage().contains(javaExecutable.getAbsolutePath()));
        }
    }

    @Test
    public void resolveZipEntryAllowsContainedPath() throws Exception {
        File root = temporaryFolder.newFolder("extract");

        File resolved = LauncherCodeGenerator.resolveZipEntry(root, "nested/file.txt");

        Assert.assertEquals(new File(root, "nested/file.txt").getCanonicalFile(), resolved);
    }

    @Test
    public void resolveZipEntryRejectsParentTraversal() throws Exception {
        assertUnsafeEntry("../outside.txt");
        assertUnsafeEntry("nested/../../outside.txt");
    }

    @Test
    public void resolveZipEntryRejectsAbsolutePath() throws Exception {
        File absolute = new File(temporaryFolder.getRoot(), "outside.txt").getAbsoluteFile();
        assertUnsafeEntry(absolute.getPath());
        assertUnsafeEntry("/outside.txt");
        assertUnsafeEntry("C:/outside.txt");
        assertUnsafeEntry("C:\\outside.txt");
    }

    private void assertUnsafeEntry(String entryName) throws Exception {
        File root = temporaryFolder.newFolder("extract-" + Math.abs(entryName.hashCode()));
        try {
            LauncherCodeGenerator.resolveZipEntry(root, entryName);
            Assert.fail("Expected unsafe ZIP path to be rejected: " + entryName);
        } catch (TransformException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains(entryName));
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
}
