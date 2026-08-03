package io.kyle.javaguard.support;

import io.kyle.javaguard.bean.AppConfig;
import io.kyle.javaguard.bean.KeyInfo;
import io.kyle.javaguard.bean.SignatureInfo;
import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.exception.TransformException;
import io.kyle.javaguard.util.*;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.LocalFileHeader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/8 22:29
 */
public class LauncherCodeGenerator {
    private static final Class<?>[] WRITE_RUNTIME_CLASS = {TinyHeadInputStream.class, InternalResourceDecryptInputStream.class, InternalResourceURLConnection.class};
    private static final String LAUNCHER_CODE_DIR = "jg-launcher-source";
    private static final String LAUNCHER_CODE_BUILD_CONFIG_FILE = "build_config.rs";
    private static final String LAUNCHER_BUILD_PATH = "build";
    private static final String LAUNCHER_CODE_BUILD_CONFIG_PATH = LAUNCHER_BUILD_PATH + File.separatorChar + LAUNCHER_CODE_BUILD_CONFIG_FILE;
    private static final String LAUNCHER_CLASS_DIR_PATH = LAUNCHER_BUILD_PATH + File.separatorChar + "ext";
    private static final String LAUNCHER_RUNTIME_CLASS_FILE = "runtime.classes";
    private static final String LAUNCHER_TRANSFORM_MOD_FILE = "transform.mod";

    private static final String[] BUILD_COMMAND = { "cargo", "build", "--release", "--locked" };

    public static void generate(String output, TransformInfo info) throws TransformException {
        AppConfig config = info.getConfig();
        File outputDir = requireDirectory(new File(output), "output directory");
        File launcherDir = new File(outputDir, LAUNCHER_CODE_DIR);
        File buildTargetDir = new File(launcherDir, "target"+File.separatorChar+"release");
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
        String execSuffix = isWindows ? ".exe" : "";

        if (launcherDir.exists()) {
            try {
                FileUtils.forceDelete(launcherDir);
            } catch (IOException e) {
                throw new TransformException("Failed to clean launcher source directory: " + launcherDir.getAbsolutePath(), e);
            }
        }
        requireDirectory(launcherDir, "launcher source directory");
        System.out.println("INFO: Extracting launcher source...");
        zipResourceExt("/jg-launcher.zip", launcherDir, false);

        if (!config.isSkipDeps()) {
            System.out.println("INFO: Extracting launcher dependencies...");
            zipResourceExt("/jg-launcher-deps.zip", launcherDir, true);
        }

        generateClass(launcherDir, info);

        // 中文：构建配置在资源释放后注入运行时密钥与类信息。 English: Inject runtime keys and class metadata after extracting launcher resources.
        Map<String, String> valueMap = postBuild(launcherDir, info);
        StringSubstitutor substitutor = new StringSubstitutor(valueMap);
        configGenerate(LAUNCHER_CODE_BUILD_CONFIG_FILE, new File(launcherDir, LAUNCHER_CODE_BUILD_CONFIG_PATH), substitutor);

        File binDir = requireDirectory(new File(outputDir, "bin"), "launcher output directory");
        File binFile = new File(buildTargetDir, "jg-launcher" + execSuffix);
        File jgJavaPkgFile = null;
        if (config.isGenLauncher()) {
            System.out.println("INFO: Building launcher...");
            executeCommand(launcherDir, BUILD_COMMAND);
            requireRegularFile(binFile, "launcher executable");
            JgFileUtils.copyFileToDirectory(binFile, binDir);
            System.out.println("INFO: Packaging Java runtime...");
            jgJavaPkgFile = packageJava(config, isWindows, outputDir, binFile, false);
        }

        System.out.println("\n--------------------------------");
        if (jgJavaPkgFile != null && jgJavaPkgFile.exists()) {
            System.out.println("INFO: Java package path: " + jgJavaPkgFile.getAbsolutePath());
        }
    }

    private static File packageJava(AppConfig config, boolean isWindows, File outputDir, File launcherBinFile, boolean isDev) throws TransformException {
        requireRegularFile(launcherBinFile, "launcher executable");
        String oriJava = config.getOriJava();
        File oriJavaFile;
        try {
            if (StringUtils.isBlank(oriJava) || !(oriJavaFile = new File(oriJava)).exists()) {
                oriJava = System.getenv("ORI_JAVA");
                if (StringUtils.isBlank(oriJava) || !(oriJavaFile = new File(oriJava)).exists()) {
                    oriJava = System.getenv("JAVA_HOME");
                    if (StringUtils.isBlank(oriJava) || !(oriJavaFile = new File(oriJava)).exists()) {
                        oriJava = System.getProperty("java.home");
                        if (StringUtils.isBlank(oriJava) || !(oriJavaFile = new File(oriJava)).exists()) {
                            System.err.println("ERROR: Java runtime path does not exist: " + oriJava);
                            FileUtils.copyFile(launcherBinFile, new File(outputDir, launcherBinFile.getName()));
                            return null;
                        }
                    }
                }
            }
            String jgJavaFileName = (isDev ? "jg-dev-" : "jg-") + oriJavaFile.getName();

            File jgJavaPkg;
            if (oriJavaFile.isDirectory()) {
                if (isWindows) {
                    JgFileUtils.zipJavaDirectory(oriJavaFile, jgJavaPkg = new File(outputDir, jgJavaFileName + ".zip"), launcherBinFile);
                } else {
                    JgFileUtils.tarGzJavaDirectory(oriJavaFile, jgJavaPkg = new File(outputDir, jgJavaFileName + ".tar.gz"), launcherBinFile);
                }
            } else if (Files.isRegularFile(oriJavaFile.toPath())) {
                jgJavaPkg = new File(outputDir, jgJavaFileName);
                if (oriJava.endsWith(".zip")) {
                    JgFileUtils.zipJava(oriJavaFile, jgJavaPkg, launcherBinFile);
                } else if (oriJava.endsWith(".tar.gz") || oriJava.endsWith(".tgz")) {
                    JgFileUtils.tarGzJava(oriJavaFile, jgJavaPkg, launcherBinFile);
                } else {
                    System.err.println("WARN: Unsupported Java runtime archive type; expected .zip, .tar.gz, or .tgz: " + oriJavaFile.getPath());
                    return null;
                }
            } else {
                System.err.println("WARN: Java runtime path is not readable: " + oriJavaFile.getPath());
                return null;
            }
            return jgJavaPkg;
        } catch (IOException e) {
            throw new TransformException("Failed to package Java runtime: " + oriJava, e);
        }
    }

    private static void zipResourceExt(String resourceName, File dir, boolean isDeps) throws TransformException {
        PrintUtils printUtils = new PrintUtils();
        URL resource = LauncherCodeGenerator.class.getResource(resourceName);
        if (resource == null) {
            if (isDeps) {
                return;
            }
            throw new TransformException("Embedded launcher resource not found: " + resourceName);
        }
        try (InputStream resourceStream = LauncherCodeGenerator.class.getResourceAsStream(resourceName);
                ZipInputStream zip = new ZipInputStream(resourceStream)) {
            LocalFileHeader entry;

            while ((entry = zip.getNextEntry()) != null) {
                String fileName = entry.getFileName();
                File file = resolveZipEntry(dir, fileName);
                if (entry.isDirectory()) {
                    requireDirectory(file, "embedded resource directory " + fileName);
                } else {
                    requireDirectory(file.getParentFile(), "embedded resource parent directory " + fileName);
                    printUtils.printInline("INFO: Extracting [%s]...", fileName);
                    FileUtils.copyToFile(zip, file);
                    if (isDeps && StringUtils.contains(fileName, "tikv-jemalloc-sys") &&
                            (StringUtils.endsWithAny(fileName, ".sh", "/configure", "/config.guess", "/config.sub", "/install-sh"))) {
                        if (!file.setExecutable(true)) {
                            System.err.println("WARN: Failed to make extracted script executable: " + fileName);
                        }
                    }
                }
            }
        } catch (TransformException e) {
            throw e;
        } catch (Exception e) {
            throw new TransformException("Failed to extract embedded resource: " + resourceName, e);
        }
        printUtils.over();
        System.out.println("INFO: Resource [" + resourceName + "] extracted successfully!");
    }

    static void executeCommand(File launcherDir, String[] cmd) throws TransformException {
        if (cmd == null || cmd.length == 0) {
            throw new TransformException("Cannot execute an empty command");
        }
        requireExistingDirectory(launcherDir, "command working directory");
        String command = String.join(" ", cmd);
        try {
            int exitCode = new ProcessBuilder(cmd)
                    .directory(launcherDir)
                    .inheritIO()
                    .start()
                    .waitFor();
            if (exitCode != 0) {
                throw new TransformException("Command failed with exit code " + exitCode + ": " + command);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransformException("Interrupted while waiting for command: " + command, e);
        } catch (IOException e) {
            throw new TransformException("Failed to start command: " + command, e);
        }
    }

    static File resolveZipEntry(File destinationDir, String entryName) throws TransformException {
        if (entryName == null || entryName.trim().isEmpty()) {
            throw new TransformException("Embedded ZIP contains an empty entry path");
        }
        try {
            File root = destinationDir.getCanonicalFile();
            File entry = new File(entryName);
            if (entry.isAbsolute() || entryName.startsWith("/") || entryName.startsWith("\\")
                    || (entryName.length() >= 3 && Character.isLetter(entryName.charAt(0))
                    && entryName.charAt(1) == ':'
                    && (entryName.charAt(2) == '/' || entryName.charAt(2) == '\\'))) {
                throw new TransformException("Embedded ZIP contains an absolute entry path: " + entryName);
            }
            File target = new File(root, entryName).getCanonicalFile();
            String rootPath = root.getPath();
            if (!target.getPath().equals(rootPath)
                    && !target.getPath().startsWith(rootPath + File.separator)) {
                throw new TransformException("Embedded ZIP entry escapes the destination directory: " + entryName);
            }
            return target;
        } catch (IOException e) {
            throw new TransformException("Failed to validate embedded ZIP entry path: " + entryName, e);
        }
    }

    private static File requireDirectory(File dir, String description) throws TransformException {
        if (dir.exists()) {
            return requireExistingDirectory(dir, description);
        }
        try {
            FileUtils.forceMkdir(dir);
            return requireExistingDirectory(dir, description);
        } catch (IOException e) {
            throw new TransformException("Failed to create " + description + ": " + dir.getAbsolutePath(), e);
        }
    }

    private static File requireExistingDirectory(File dir, String description) throws TransformException {
        if (!dir.isDirectory()) {
            throw new TransformException(description + " does not exist or is not a directory: " + dir.getAbsolutePath());
        }
        return dir;
    }

    private static File requireRegularFile(File file, String description) throws TransformException {
        if (!Files.isRegularFile(file.toPath())) {
            throw new TransformException(description + " does not exist or is not a regular file: " + file.getAbsolutePath());
        }
        return file;
    }

    private static Map<String, String> postBuild(File launcherDir, TransformInfo info) throws TransformException {
        KeyInfo keyInfo = info.getKeyInfo();
        KeyInfo resourceKeyInfo = info.getResourceKeyInfo();
        SignatureInfo signatureInfo = info.getSignature();
        HashMap<String, String> valueMap = new HashMap<>(4);
        valueMap.put("key", bytesToString(keyInfo.getKey()));
        valueMap.put("resourceKey", bytesToString(resourceKeyInfo.getKey()));
        valueMap.put("publicKey", bytesToString(signatureInfo.getPublicKey().getEncoded()));
        valueMap.put("privateKey", bytesToString(signatureInfo.getPrivateKey().getEncoded()));
        valueMap.put("signKeyVersion", signatureInfo.getKeyHash());
        SFunction<URLConnection, URLConnection> handleConnection = InternalResourceURLConnection::handleConnection;
        valueMap.put("internalUrlConnectionClass", LambdaUtils.getClassName(handleConnection));
        valueMap.put("internalUrlConnectionMethod", LambdaUtils.getMethodName(handleConnection));
        valueMap.put("internalUrlConnectionDesc", LambdaUtils.getMethodDescriptor(handleConnection));

        valueMap.put("decryptNativeClass", InternalResourceDecryptInputStream.class.getName().replace(".", "/"));
        valueMap.put("decryptNativeMethod", "transformer");
        valueMap.put("decryptNativeDesc", "([BII)I");
        return valueMap;
    }

    private static void configGenerate(String resourceName, File configFile, StringSubstitutor substitutor) throws TransformException {
        try (InputStream configRs = LauncherCodeGenerator.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (configRs == null) {
                throw new TransformException("Launcher build configuration resource not found: " + resourceName);
            }
            FileUtils.write(configFile,
                    substitutor.replace(IOUtils.toString(configRs, StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        } catch (TransformException e) {
            throw e;
        } catch (IOException e) {
            throw new TransformException("Failed to read or write launcher build configuration: " + resourceName
                    + " -> " + configFile.getAbsolutePath(), e);
        }
    }

    private static void generateClass(File launcherDir, TransformInfo info) throws TransformException {
        File launcherClassDir = requireDirectory(new File(launcherDir, LAUNCHER_CLASS_DIR_PATH), "launcher class output directory");

        // 中文：运行时类按名称长度、名称、字节码长度、字节码依次序列化。 English: Serialize runtime classes as name length, name, bytecode length, and bytecode.
        File out = new File(launcherClassDir, LAUNCHER_RUNTIME_CLASS_FILE);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                OutputStream fileOutput = Files.newOutputStream(out.toPath())) {
            for (Class<?> clazz : WRITE_RUNTIME_CLASS) {
                // 中文：保留预编译类以匹配当前启动器格式；后续可改为运行时编译。 English: Keep precompiled classes for the current launcher format; runtime compilation remains future work.
                try (InputStream stream = clazz.getResourceAsStream(clazz.getSimpleName() + ".class")) {
                    byte[] name = clazz.getName().replace(".", "/").getBytes(StandardCharsets.UTF_8);
                    assert stream != null;
                    byte[] byteArray = IOUtils.toByteArray(stream);
                    outputStream.write(BytesUtils.intToBytes(name.length));
                    outputStream.write(name);
                    outputStream.write(BytesUtils.intToBytes(byteArray.length));
                    outputStream.write(byteArray);
                    IOUtils.copy(stream, outputStream);
                }
            }
            ByteArrayInputStream stream = new ByteArrayInputStream(outputStream.toByteArray());
            IOUtils.copy(stream, fileOutput);
        } catch (Exception e) {
            throw new TransformException("Failed to write runtime classes", e);
        }

    }

    private static String bytesToString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "[]";
        }
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (byte b : bytes) {
            joiner.add(Byte.toUnsignedInt(b) + "u8");
        }
        return joiner.toString();
    }
}
