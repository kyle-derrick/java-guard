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

    private static final String[] BUILD_COMMAND = { "cargo", "build", "--release" };

    public static void generate(String output, TransformInfo info) throws TransformException {
        AppConfig config = info.getConfig();
        File launcherDir = new File(output, LAUNCHER_CODE_DIR);
        File buildTargetDir = new File(launcherDir, "target"+File.separatorChar+"release");
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
        String execSuffix = isWindows ? ".exe" : "";

        if (launcherDir.exists()) {
            try {
                FileUtils.forceDelete(launcherDir);
            } catch (IOException e) {
                System.err.println("ERROR: Could not delete " + launcherDir);
            }
        }

        try {
            FileUtils.forceMkdir(launcherDir);
        } catch (IOException e) {
            throw new TransformException("create jg-launcher dir failed!", e);
        }
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

        File binDir = new File(output, "bin");
        if (!binDir.exists()) {
            binDir.mkdirs();
        }
        File binFile = new File(buildTargetDir, "jg-launcher" + execSuffix);
        File jgJavaPkgFile = null;
        if (config.isGenLauncher()) {
            System.out.println("开始编译启动器...");
            executeCommand(launcherDir, BUILD_COMMAND);
            JgFileUtils.copyFileToDirectory(binFile, binDir);
            System.out.println("开始打包Java环境...");
            jgJavaPkgFile = packageJava(config, isWindows, output, binFile, false);
        }

        System.out.println("\n--------------------------------");
        if (jgJavaPkgFile != null && jgJavaPkgFile.exists()) {
            System.out.println(">> Java 环境包路径：" + jgJavaPkgFile.getAbsolutePath());
        }
    }

    private static File packageJava(AppConfig config, boolean isWindows, String output, File launcherBinFile, boolean isDev) throws TransformException {
        if (!launcherBinFile.exists()) {
            System.err.println("未找到编译后的启动器：" + launcherBinFile.getAbsolutePath());
            return null;
        }
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
                            FileUtils.copyFile(launcherBinFile, new File(output, launcherBinFile.getName()));
                            return null;
                        }
                    }
                }
            }
            String jgJavaFileName = (isDev ? "jg-dev-" : "jg-") + oriJavaFile.getName();

            File jgJavaPkg;
            if (oriJavaFile.isDirectory()) {
                if (isWindows) {
                    JgFileUtils.zipJavaDirectory(oriJavaFile, jgJavaPkg = new File(output, jgJavaFileName + ".zip"), launcherBinFile);
                } else {
                    JgFileUtils.tarGzJavaDirectory(oriJavaFile, jgJavaPkg = new File(output, jgJavaFileName + ".tar.gz"), launcherBinFile);
                }
            } else if (Files.isRegularFile(oriJavaFile.toPath())) {
                jgJavaPkg = new File(output, jgJavaFileName);
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
        if (isDeps && resource == null) {
            return;
        }
        try (ZipInputStream zip = new ZipInputStream(LauncherCodeGenerator.class.getResourceAsStream(resourceName))) {
            LocalFileHeader entry;

            while ((entry = zip.getNextEntry()) != null) {
                String fileName = entry.getFileName();
                File file = new File(dir, fileName);
                if (entry.isDirectory()) {
                    try {
                        FileUtils.forceMkdir(file);
                    } catch (IOException e) {
                        throw new TransformException("create resource [" + resourceName + "] dir [" + fileName + "] failed!", e);
                    }
                } else {
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
        } catch (Exception e) {
            throw new TransformException("release resource [" + resourceName + "] failed!", e);
        }
        printUtils.over();
        System.out.println("release resource [" + resourceName + "] finished!");
    }

    private static void executeCommand(File launcherDir, String[] cmd) throws TransformException {
        try {
            ProcessBuilder processBuilderS = new ProcessBuilder(cmd)
                    .directory(launcherDir)
                    .inheritIO();
            processBuilderS.start()
                    .waitFor();
        } catch (Exception e) {
            throw new TransformException("执行命令时出错：" + String.join(" ", cmd), e);
        }
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
            assert configRs != null;
            FileUtils.write(configFile,
                    substitutor.replace(IOUtils.toString(configRs, StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new TransformException("read or write launcher build config [" + resourceName + "] failed", e);
        }
    }

    private static void generateClass(File launcherDir, TransformInfo info) throws TransformException {
        File launcherClassDir = new File(launcherDir, LAUNCHER_CLASS_DIR_PATH);
        if (!launcherClassDir.exists()) {
            launcherClassDir.mkdirs();
        }

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
