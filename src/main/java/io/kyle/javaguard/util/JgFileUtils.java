package io.kyle.javaguard.util;

import io.kyle.javaguard.exception.TransformException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/08 10:32
 */
public class JgFileUtils {
    public static void copyFile(File from, File to, File bak) throws IOException {
        if (to.exists() && !bak.exists()) {
            FileUtils.moveFile(to, bak);
        }
        FileUtils.copyFile(from, to, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void copyFile(File from, File to) throws TransformException {
        if (!from.exists()) {
            throw new TransformException("Failed to copy file; source does not exist: " + from.getPath());
        }
        try {
            FileUtils.copyFile(from, to);
        } catch (IOException e) {
            throw new TransformException("Failed to copy file: " + from.getPath() + " -> " + to.getPath(), e);
        }
    }

    public static void copyFileToDirectory(File from, File to) throws TransformException {
        if (!from.exists()) {
            throw new TransformException("Failed to copy file; source does not exist: " + from.getPath());
        }
        try {
            FileUtils.copyFileToDirectory(from, to);
        } catch (IOException e) {
            throw new TransformException("Failed to copy file: " + from.getPath() + " -> " + to.getPath(), e);
        }
    }

    private static ZipArchiveEntry zipJavaBinHandle(ZipArchiveEntry zipEntry, ZipArchiveOutputStream zipOut,
                                                     File binFile, String binExtension,
                                                     ReplacementTracker replacementTracker) throws IOException {
        String fullName = zipEntry.getName();
        String name = FilenameUtils.getName(fullName);
        String baseName = FilenameUtils.getBaseName(name);
        String entryExtension = FilenameUtils.getExtension(name);
        String parent = FilenameUtils.getFullPath(fullName);
        if (StringUtils.equals(baseName, "java") && StringUtils.equals(binExtension, entryExtension)
                && StringUtils.equals(FilenameUtils.getName(FilenameUtils.normalizeNoEndSeparator(parent)), "bin")) {
            zipOut.putArchiveEntry(new ZipArchiveEntry(binFile, fullName));
            FileUtils.copyFile(binFile, zipOut);
            zipOut.closeArchiveEntry();

            String oriName = parent + baseName + "_ori" + (StringUtils.isEmpty(binExtension) ? StringUtils.EMPTY : ("." + binExtension));
            ZipArchiveEntry oriZipEntry = new ZipArchiveEntry(oriName);
            oriZipEntry.setComment(zipEntry.getComment());
            oriZipEntry.setTime(zipEntry.getTime());
            oriZipEntry.setSize(zipEntry.getSize());
            if (zipEntry.getCreationTime() != null) {
                oriZipEntry.setCreationTime(zipEntry.getCreationTime());
            }
            if (zipEntry.getLastModifiedTime() != null) {
                oriZipEntry.setLastModifiedTime(zipEntry.getLastModifiedTime());
            }
            oriZipEntry.setRawFlag(zipEntry.getRawFlag());
            oriZipEntry.setUnixMode(zipEntry.getUnixMode());
            int method = zipEntry.getMethod();
            if (method >= 0) {
                oriZipEntry.setMethod(method);
                if (oriZipEntry.getMethod() == ZipEntry.STORED) {
                    oriZipEntry.setCrc(zipEntry.getCrc());
                    oriZipEntry.setCompressedSize(zipEntry.getCompressedSize());
                }
            }

            zipEntry = oriZipEntry;
            replacementTracker.replaced = true;
        }
        return zipEntry;
    }

    public static void zipJava(File source, File target, File binFile) throws IOException {
        checkFile(source, target);
        Path temporary = createSiblingTempFile(target.toPath());
        boolean replaced = false;
        try {
            Path sourcePath = source.toPath();
            PrintUtils printUtils = new PrintUtils();
            String extension = FilenameUtils.getExtension(binFile.getName());
            ReplacementTracker replacementTracker = new ReplacementTracker();
            try (ZipArchiveInputStream zipIn = new ZipArchiveInputStream(Files.newInputStream(sourcePath));
                 ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(Files.newOutputStream(temporary,
                         StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))) {
                ZipArchiveEntry zipEntry;
                while ((zipEntry = zipIn.getNextEntry()) != null) {
                    zipEntry = (ZipArchiveEntry) zipEntry.clone();
                    if (zipEntry.isDirectory()) {
                        zipOut.putArchiveEntry(zipEntry);
                        zipOut.closeArchiveEntry();
                        printUtils.printInline("INFO: ZIP directory %s", zipEntry.getName());
                    } else {
                        zipEntry = zipJavaBinHandle(zipEntry, zipOut, binFile, extension, replacementTracker);
                        zipOut.putArchiveEntry(zipEntry);
                        IOUtils.copy(zipIn, zipOut);
                        zipOut.closeArchiveEntry();
                        printUtils.printInline("INFO: ZIP file %s", zipEntry.getName());
                    }
                }
                zipOut.flush();
            }
            requireJavaReplacement(replacementTracker);
            safeReplace(temporary, target.toPath());
            replaced = true;
            printUtils.over();
            System.out.println("INFO: Java ZIP archive created: " + target.getPath());
        } finally {
            if (!replaced) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private static TarArchiveEntry tarGzJavaBinHandle(TarArchiveEntry tarEntry, TarArchiveOutputStream tarOut,
                                                      File binFile, String binExtension,
                                                      ReplacementTracker replacementTracker) throws IOException {
        String fullName = tarEntry.getName();
        String name = FilenameUtils.getName(fullName);
        String baseName = FilenameUtils.getBaseName(name);
        String entryExtension = FilenameUtils.getExtension(name);
        String parent = FilenameUtils.getFullPath(fullName);
        if (StringUtils.equals(baseName, "java") && StringUtils.equals(binExtension, entryExtension)
                && StringUtils.equals(FilenameUtils.getName(FilenameUtils.normalizeNoEndSeparator(parent)), "bin")) {
            TarArchiveEntry jgEntry = new TarArchiveEntry(binFile, fullName);
            jgEntry.setMode(0755);
            tarOut.putArchiveEntry(jgEntry);
            FileUtils.copyFile(binFile, tarOut);
            tarOut.closeArchiveEntry();

            String oriName = parent + baseName + "_ori" + (StringUtils.isEmpty(binExtension) ? StringUtils.EMPTY : ("." + binExtension));
            tarEntry.setName(oriName);
            replacementTracker.replaced = true;
        }
        return tarEntry;
    }

    public static void tarGzJava(File source, File target, File binFile) throws IOException {
        checkFile(source, target);
        Path temporary = createSiblingTempFile(target.toPath());
        boolean replaced = false;
        try {
            Path sourcePath = source.toPath();
            PrintUtils printUtils = new PrintUtils();
            String extension = FilenameUtils.getExtension(binFile.getName());
            ReplacementTracker replacementTracker = new ReplacementTracker();
            try (TarArchiveInputStream tarIn = new TarArchiveInputStream(new GzipCompressorInputStream(Files.newInputStream(sourcePath)));
                 TarArchiveOutputStream tarOut = new TarArchiveOutputStream(new GzipCompressorOutputStream(Files.newOutputStream(temporary,
                         StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)))) {
                tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
                TarArchiveEntry tarEntry;
                while ((tarEntry = tarIn.getNextEntry()) != null) {
                    if (tarEntry.isDirectory()) {
                        tarOut.putArchiveEntry(tarEntry);
                        tarOut.closeArchiveEntry();
                        printUtils.printInline("INFO: TAR directory %s", tarEntry.getName());
                    } else {
                        tarOut.putArchiveEntry(tarGzJavaBinHandle(tarEntry, tarOut, binFile, extension, replacementTracker));
                        IOUtils.copy(tarIn, tarOut);
                        tarOut.closeArchiveEntry();
                        printUtils.printInline("INFO: TAR file %s", tarEntry.getName());
                    }
                }
            }
            requireJavaReplacement(replacementTracker);
            safeReplace(temporary, target.toPath());
            replaced = true;
            printUtils.over();
            System.out.println("INFO: Java TAR.GZ archive created: " + target.getPath());
        } finally {
            if (!replaced) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    public static void zip(File source, File target) throws IOException {
        zip(source, target, null);
    }

    public static void zipJavaDirectory(File source, File target, File binFile) throws IOException {
        checkFile(source, target);
        Path temporary = createSiblingTempFile(target.toPath());
        boolean replaced = false;
        try {
            ReplacementTracker replacementTracker = new ReplacementTracker();
            zip(source, temporary.toFile(), ((entry, out) ->
                    zipJavaBinHandle(entry, out, binFile, FilenameUtils.getExtension(binFile.getName()), replacementTracker)
            ));
            requireJavaReplacement(replacementTracker);
            safeReplace(temporary, target.toPath());
            replaced = true;
        } finally {
            if (!replaced) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    public static void zip(File source, File target, EntryHandler<ZipArchiveEntry, ZipArchiveOutputStream> entryHandler) throws IOException {
        checkFile(source, target);
        Path temporary = createSiblingTempFile(target.toPath());
        boolean replaced = false;
        try {
            writeZip(source, temporary.toFile(), entryHandler);
            safeReplace(temporary, target.toPath());
            replaced = true;
        } finally {
            if (!replaced) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private static void writeZip(File source, File target,
                                 EntryHandler<ZipArchiveEntry, ZipArchiveOutputStream> entryHandler) throws IOException {
        Path sourcePath = source.toPath();
        PrintUtils printUtils = new PrintUtils();
        try (ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(Files.newOutputStream(target.toPath(),
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE));
             Stream<Path> filePaths = Files.walk(sourcePath)) {
            ZipParameters parameters = new ZipParameters();
            parameters.setCompressionLevel(CompressionLevel.NORMAL);
            parameters.setCompressionMethod(CompressionMethod.DEFLATE);
            parameters.setIncludeRootFolder(false);

            Path sourcePathParent = sourcePath.getParent();
            Iterator<Path> iterator = filePaths.iterator();
            while (iterator.hasNext()) {
                Path filePath = iterator.next();
                Path relativePath = sourcePathParent.relativize(filePath);
                String entryName = toArchiveName(relativePath);

                if (Files.isDirectory(filePath)) {
                    ZipArchiveEntry entry = new ZipArchiveEntry(filePath, entryName);
                    zipOut.putArchiveEntry(entry);
                    zipOut.closeArchiveEntry();
                    printUtils.printInline("INFO: ZIP directory %s", filePath.toString());
                } else if (Files.isRegularFile(filePath)) {
                    ZipArchiveEntry entry = new ZipArchiveEntry(filePath, entryName);

                    if (entryHandler != null) {
                        entry = entryHandler.handle(entry, zipOut);
                    }

                    zipOut.putArchiveEntry(entry);

                    FileUtils.copyFile(filePath.toFile(), zipOut);
                    zipOut.closeArchiveEntry();
                    printUtils.printInline("INFO: ZIP file %s", filePath.toString());
                } else {
                    System.err.println("\nWARN: Skipping " + filePath + " because it is not a directory or a file");
                }
            }
            zipOut.flush();
        }
        printUtils.over();
        System.out.println("INFO: ZIP archive created: " + target.getPath());
    }

    public static void tarGz(File source, File target) throws IOException {
        tarGz(source, target, null);
    }

    public static void tarGzJavaDirectory(File source, File target, File binFile) throws IOException {
        checkFile(source, target);
        Path temporary = createSiblingTempFile(target.toPath());
        boolean replaced = false;
        try {
            ReplacementTracker replacementTracker = new ReplacementTracker();
            tarGz(source, temporary.toFile(), ((entry, out) ->
                tarGzJavaBinHandle(entry, out, binFile, FilenameUtils.getExtension(binFile.getName()), replacementTracker)
            ));
            requireJavaReplacement(replacementTracker);
            safeReplace(temporary, target.toPath());
            replaced = true;
        } finally {
            if (!replaced) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    public static void tarGz(File source, File target, EntryHandler<TarArchiveEntry, TarArchiveOutputStream> entryHandler) throws IOException {
        checkFile(source, target);
        Path temporary = createSiblingTempFile(target.toPath());
        boolean replaced = false;
        try {
            writeTarGz(source, temporary.toFile(), entryHandler);
            safeReplace(temporary, target.toPath());
            replaced = true;
        } finally {
            if (!replaced) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private static void writeTarGz(File source, File target,
                                   EntryHandler<TarArchiveEntry, TarArchiveOutputStream> entryHandler) throws IOException {
        Path sourcePath = source.toPath();
        PrintUtils printUtils = new PrintUtils();
        try (TarArchiveOutputStream tarOut = new TarArchiveOutputStream(new GzipCompressorOutputStream(Files.newOutputStream(target.toPath(),
                     StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)));
             Stream<Path> filePaths = Files.walk(sourcePath)) {
            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            Path sourcePathParent = sourcePath.getParent();
            Iterator<Path> iterator = filePaths.iterator();
            while (iterator.hasNext()) {
                Path filePath = iterator.next();
                Path relativePath = sourcePathParent.relativize(filePath);
                String entryName = toArchiveName(relativePath);

                if (Files.isSymbolicLink(filePath)) {
                    TarArchiveEntry linkEntry = new TarArchiveEntry(entryName, true);
                    Path linkTarget = Files.readSymbolicLink(filePath);
                    linkEntry.setLinkName(toArchiveName(linkTarget));
                    linkEntry.setMode(0777);
                    tarOut.putArchiveEntry(linkEntry);
                    tarOut.closeArchiveEntry();
                    printUtils.printInline("INFO: Creating link: %s", filePath.toString());
                } else if (Files.isDirectory(filePath)) {
                    TarArchiveEntry entry = new TarArchiveEntry(filePath, entryName);
                    tarOut.putArchiveEntry(entry);
                    tarOut.closeArchiveEntry();
                    printUtils.printInline("INFO: Adding TAR directory: %s", filePath.toString());
                } else if (Files.isRegularFile(filePath)) {
                    TarArchiveEntry entry = new TarArchiveEntry(filePath, entryName);
                    int mode;
                    if (Files.isExecutable(filePath)) {
                        mode = 0755;
                    } else {
                        mode = 0644;
                    }
                    entry.setMode(mode);
                    if (entryHandler != null) {
                        entry = entryHandler.handle(entry, tarOut);
                    }
                    tarOut.putArchiveEntry(entry);
                    FileUtils.copyFile(filePath.toFile(), tarOut);
                    tarOut.closeArchiveEntry();
                    printUtils.printInline("INFO: Adding TAR entry: %s", filePath.toString());
                } else {
                    System.err.println("\nWARN: Skipping " + filePath + " because it is not a directory or a file");
                }
            }

            printUtils.over();
            System.out.println("INFO: TAR.GZ archive created: " + target.getPath());
        }
    }

    private static void checkFile(File source, File target) throws IOException {
        if (!source.exists()) {
            throw new FileNotFoundException(source.getPath());
        }
        if (target.exists() && target.isDirectory()) {
            throw new IOException("Target is a directory: " + target.getPath());
        }
    }

    private static String toArchiveName(Path path) {
        return path.toString().replace(File.separatorChar, '/');
    }

    private static Path createSiblingTempFile(Path target) throws IOException {
        Path absoluteTarget = target.toAbsolutePath();
        Path parent = absoluteTarget.getParent();
        if (parent == null) {
            parent = new File(".").toPath().toAbsolutePath();
        }
        Files.createDirectories(parent);
        String fileName = absoluteTarget.getFileName().toString();
        String prefix = fileName.length() >= 3 ? fileName : "jg-" + fileName;
        return Files.createTempFile(parent, "." + prefix + "-", ".tmp");
    }

    public static void safeReplace(Path source, Path destination) throws IOException {
        safeReplace(source, destination, Files::move);
    }

    static void safeReplace(Path source, Path destination, FileMover mover) throws IOException {
        try {
            mover.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return;
        } catch (AtomicMoveNotSupportedException ignored) {
            // 中文：原子移动不可用时，使用可回滚的非原子替换。 English: Fall back to rollback-capable non-atomic replacement.
        }

        Path backup = null;
        boolean destinationBackedUp = false;
        try {
            if (Files.exists(destination)) {
                backup = createSiblingTempFile(destination);
                Files.delete(backup);
                mover.move(destination, backup);
                destinationBackedUp = true;
            }
            try {
                mover.move(source, destination);
            } catch (IOException replacementFailure) {
                if (destinationBackedUp) {
                    try {
                        Files.deleteIfExists(destination);
                        mover.move(backup, destination);
                        destinationBackedUp = false;
                    } catch (IOException rollbackFailure) {
                        replacementFailure.addSuppressed(rollbackFailure);
                    }
                }
                throw replacementFailure;
            }
            destinationBackedUp = false;
            if (backup != null) {
                try {
                    Files.deleteIfExists(backup);
                } catch (IOException cleanupFailure) {
                    backup.toFile().deleteOnExit();
                }
            }
        } finally {
            if (!destinationBackedUp && backup != null) {
                try {
                    Files.deleteIfExists(backup);
                } catch (IOException cleanupFailure) {
                    backup.toFile().deleteOnExit();
                }
            }
        }
    }

    @FunctionalInterface
    interface FileMover {
        Path move(Path source, Path destination, java.nio.file.CopyOption... options) throws IOException;
    }

    private static void requireJavaReplacement(ReplacementTracker replacementTracker) throws IOException {
        if (!replacementTracker.replaced) {
            throw new IOException("Java package does not contain a matching bin/java launcher");
        }
    }

    private static final class ReplacementTracker {
        private boolean replaced;
    }

    @FunctionalInterface
    public interface EntryHandler<T extends ArchiveEntry, O extends ArchiveOutputStream<T>> {
        T handle(T entry, O out) throws IOException;
    }
}
