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
        File outputDir = requireDirectory(new File(output), "输出目录");
        File launcherDir = new File(outputDir, LAUNCHER_CODE_DIR);
        File buildTargetDir = new File(launcherDir, "target"+File.separatorChar+"release");
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
        String execSuffix = isWindows ? ".exe" : "";

        if (launcherDir.exists()) {
            try {
                FileUtils.forceDelete(launcherDir);
            } catch (IOException e) {
                throw new TransformException("清理启动器源码目录失败: " + launcherDir.getAbsolutePath(), e);
            }
        }
        requireDirectory(launcherDir, "启动器源码目录");
        System.out.println("释放启动器源码中...");
        zipResourceExt("/jg-launcher.zip", launcherDir, false);

        if (!config.isSkipDeps()) {
            System.out.println("释放启动器构建依赖中...");
            zipResourceExt("/jg-launcher-deps.zip", launcherDir, true);
        }

        generateClass(launcherDir, info);

        // todo copy to launcher dir
        Map<String, String> valueMap = postBuild(launcherDir, info);
        StringSubstitutor substitutor = new StringSubstitutor(valueMap);
        configGenerate(LAUNCHER_CODE_BUILD_CONFIG_FILE, new File(launcherDir, LAUNCHER_CODE_BUILD_CONFIG_PATH), substitutor);

        File binDir = requireDirectory(new File(outputDir, "bin"), "启动器输出目录");
        File binFile = new File(buildTargetDir, "jg-launcher" + execSuffix);
        File jgJavaPkgFile = null;
        if (config.isGenLauncher()) {
            System.out.println("开始编译启动器...");
            executeCommand(launcherDir, BUILD_COMMAND);
            requireRegularFile(binFile, "编译后的启动器");
            JgFileUtils.copyFileToDirectory(binFile, binDir);
            System.out.println("开始打包Java环境...");
            jgJavaPkgFile = packageJava(config, isWindows, outputDir, binFile, false);
        }

        System.out.println("\n--------------------------------");
        if (jgJavaPkgFile != null && jgJavaPkgFile.exists()) {
            System.out.println(">> Java 环境包路径：" + jgJavaPkgFile.getAbsolutePath());
        }
    }

    private static File packageJava(AppConfig config, boolean isWindows, File outputDir, File launcherBinFile, boolean isDev) throws TransformException {
        requireRegularFile(launcherBinFile, "用于打包Java环境的启动器");
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
                            System.err.println("Java包文件不存在：" + oriJava);
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
                    System.err.println("WARN: 未知的Java环境包类型，请指定 .zip 或 .tar.gz/.tgz，现在是：" + oriJavaFile.getPath());
                    return null;
                }
            } else {
                System.err.println("WARN: 无法读取的Java环境路径：" + oriJavaFile.getPath());
                return null;
            }
            return jgJavaPkg;
        } catch (IOException e) {
            throw new TransformException("打包Java环境出错: " + oriJava, e);
        }
    }

    private static void zipResourceExt(String resourceName, File dir, boolean isDeps) throws TransformException {
        PrintUtils printUtils = new PrintUtils();
        URL resource = LauncherCodeGenerator.class.getResource(resourceName);
        if (resource == null) {
            if (isDeps) {
                return;
            }
            throw new TransformException("未找到启动器内嵌资源: " + resourceName);
        }
        try (InputStream resourceStream = LauncherCodeGenerator.class.getResourceAsStream(resourceName);
                ZipInputStream zip = new ZipInputStream(resourceStream)) {
            LocalFileHeader entry;

            while ((entry = zip.getNextEntry()) != null) {
                String fileName = entry.getFileName();
                File file = resolveZipEntry(dir, fileName);
                if (entry.isDirectory()) {
                    requireDirectory(file, "内嵌资源目录 " + fileName);
                } else {
                    requireDirectory(file.getParentFile(), "内嵌资源父目录 " + fileName);
                    printUtils.printInline("释放[%s]...", fileName);
                    FileUtils.copyToFile(zip, file);
                    if (isDeps && StringUtils.contains(fileName, "tikv-jemalloc-sys") &&
                            (StringUtils.endsWithAny(fileName, ".sh", "/configure", "/config.guess", "/config.sub", "/install-sh"))) {
                        if (!file.setExecutable(true)) {
                            System.err.println("WARN: 释放脚本添加可执行权限失败: " + fileName);
                        }
                    }
                }
            }
        } catch (TransformException e) {
            throw e;
        } catch (Exception e) {
            throw new TransformException("释放内嵌资源失败: " + resourceName, e);
        }
        printUtils.over();
        System.out.println("release resource [" + resourceName + "] finished!");
    }

    static void executeCommand(File launcherDir, String[] cmd) throws TransformException {
        if (cmd == null || cmd.length == 0) {
            throw new TransformException("无法执行空命令");
        }
        requireExistingDirectory(launcherDir, "命令工作目录");
        String command = String.join(" ", cmd);
        try {
            int exitCode = new ProcessBuilder(cmd)
                    .directory(launcherDir)
                    .inheritIO()
                    .start()
                    .waitFor();
            if (exitCode != 0) {
                throw new TransformException("命令执行失败（退出码 " + exitCode + "）: " + command);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransformException("等待命令执行时被中断: " + command, e);
        } catch (IOException e) {
            throw new TransformException("无法启动命令: " + command, e);
        }
    }

    static File resolveZipEntry(File destinationDir, String entryName) throws TransformException {
        if (entryName == null || entryName.trim().isEmpty()) {
            throw new TransformException("内嵌ZIP包含空路径条目");
        }
        try {
            File root = destinationDir.getCanonicalFile();
            File entry = new File(entryName);
            if (entry.isAbsolute() || entryName.startsWith("/") || entryName.startsWith("\\")
                    || (entryName.length() >= 3 && Character.isLetter(entryName.charAt(0))
                    && entryName.charAt(1) == ':'
                    && (entryName.charAt(2) == '/' || entryName.charAt(2) == '\\'))) {
                throw new TransformException("内嵌ZIP包含绝对路径条目: " + entryName);
            }
            File target = new File(root, entryName).getCanonicalFile();
            String rootPath = root.getPath();
            if (!target.getPath().equals(rootPath)
                    && !target.getPath().startsWith(rootPath + File.separator)) {
                throw new TransformException("内嵌ZIP条目逃逸目标目录: " + entryName);
            }
            return target;
        } catch (IOException e) {
            throw new TransformException("无法校验内嵌ZIP条目路径: " + entryName, e);
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
            throw new TransformException("创建" + description + "失败: " + dir.getAbsolutePath(), e);
        }
    }

    private static File requireExistingDirectory(File dir, String description) throws TransformException {
        if (!dir.isDirectory()) {
            throw new TransformException(description + "不存在或不是目录: " + dir.getAbsolutePath());
        }
        return dir;
    }

    private static File requireRegularFile(File file, String description) throws TransformException {
        if (!Files.isRegularFile(file.toPath())) {
            throw new TransformException(description + "不存在或不是普通文件: " + file.getAbsolutePath());
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
                throw new TransformException("未找到启动器构建配置资源: " + resourceName);
            }
            FileUtils.write(configFile,
                    substitutor.replace(IOUtils.toString(configRs, StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        } catch (TransformException e) {
            throw e;
        } catch (IOException e) {
            throw new TransformException("读写启动器构建配置失败: " + resourceName
                    + " -> " + configFile.getAbsolutePath(), e);
        }
    }

    private static void generateClass(File launcherDir, TransformInfo info) throws TransformException {
        File launcherClassDir = requireDirectory(new File(launcherDir, LAUNCHER_CLASS_DIR_PATH), "启动器类输出目录");

        // runtime classes
        File out = new File(launcherClassDir, LAUNCHER_RUNTIME_CLASS_FILE);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                OutputStream fileOutput = Files.newOutputStream(out.toPath())) {
            for (Class<?> clazz : WRITE_RUNTIME_CLASS) {
                // todo 改为运行时编译
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
            throw new TransformException("write runtime class failed", e);
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
