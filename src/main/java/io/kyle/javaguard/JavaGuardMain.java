package io.kyle.javaguard;

import io.kyle.javaguard.bean.AppConfig;
import io.kyle.javaguard.bean.KeyInfo;
import io.kyle.javaguard.bean.SignatureInfo;
import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.constant.ConstVars;
import io.kyle.javaguard.constant.TransformType;
import io.kyle.javaguard.exception.TransformException;
import io.kyle.javaguard.support.LauncherCodeGenerator;
import io.kyle.javaguard.transform.JarTransformer;
import io.kyle.javaguard.util.JgFileUtils;
import io.kyle.javaguard.util.ZipSignUtils;
import org.apache.commons.cli.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.yaml.snakeyaml.Yaml;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Arrays;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/08 10:32
 */
public class JavaGuardMain {
    private static final Option CONFIG_OPTION =
            new Option("c", "config", true, "config files (default: ./config.yml)");
    private static final Option MODE_OPTION =
            new Option("m", "mode", true, "encrypt/decrypt/signature mode (default encrypt)");
    private static final Option OUTPUT_OPTION =
            new Option("o", "output", true, "output dir");
    private static final Option GENERATE_LAUNCHER_OPTION =
            new Option("l", "launcher", false, "generate jg launcher");
//    private static final Option DEV_LAUNCHER_OPTION =
//            new Option("ld", "l-dev", false, "generate jg launcher with dev feature");
    private static final Option SKIP_DEPS_OPTION =
            new Option(null, "skip-deps", false, "skip deps release");
    private static final Option HELP_OPTION =
            new Option("h", "help", false, "print usage");
    private static final Options OPTIONS = new Options()
            .addOption(CONFIG_OPTION)
            .addOption(MODE_OPTION)
            .addOption(OUTPUT_OPTION)
            .addOption(GENERATE_LAUNCHER_OPTION)
//            .addOption(DEV_LAUNCHER_OPTION)
            .addOption(SKIP_DEPS_OPTION)
            .addOption(HELP_OPTION);

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) {
        int result = run(args);
        if (result != 0) {
            throw new IllegalStateException("java-guard failed with exit code " + result);
        }
    }

    static int run(String[] args) {
        if (args.length == 0) {
            printUsage();
            return 1;
        }
        CommandLine parse;
        try {
            parse = parseArgs(args);
        } catch (ParseException e) {
            System.err.println("invalid arguments: " + e.getMessage());
            printUsage();
            return 1;
        }
        if (parse.hasOption(HELP_OPTION)) {
            printUsage();
            return 0;
        }
        AppConfig appConfig;
        try {
            appConfig = appArgs(parse);
        } catch (Exception e) {
            System.err.println("configuration failed: " + message(e));
            return 1;
        }
        String output = appConfig.getOutput();
        boolean isDecrypt = appConfig.getMode() == TransformType.decrypt;
        TransformInfo transformInfo;
        try {
            transformInfo = transformInfo(appConfig);
        } catch (RuntimeException e) {
            System.err.println("configuration failed: " + message(e));
            return 1;
        }
        if (transformInfo == null) {
            return 1;
        }
        File outputFile = new File(output);
        if (outputFile.exists() && outputFile.isFile()) {
            System.err.println("output dir is exists file: " + output);
            return 1;
        }
        if (!outputFile.exists() && !outputFile.mkdirs()) {
            System.err.println("cannot create output dir: " + output);
            return 1;
        }
        SignatureInfo signatureInfo = transformInfo.getSignature();
        String[] jars = parse.getArgs();
        if (ArrayUtils.isNotEmpty(jars)) {
            for (String arg : jars) {
                if (arg.endsWith(".jar")) {
                    File outFile = new File(outputFile, FilenameUtils.getName(arg));
                    Path tempFile = null;
                    try {
                        tempFile = Files.createTempFile(outputFile.toPath(), ".java-guard-", ".jar");
                        try (FileInputStream in = new FileInputStream(arg);
                             FileOutputStream out = new FileOutputStream(tempFile.toFile())) {
                            JarTransformer jarTransformer = new JarTransformer(transformInfo);
                            if (isDecrypt) {
                                jarTransformer.decrypt(in, out);
                            } else if (TransformType.signature != appConfig.getMode()) {
                                jarTransformer.encrypt(in, out);
                            } else {
                                IOUtils.copy(in, out);
                            }
                        }
                        ZipSignUtils.sign(tempFile.toFile(), signatureInfo.newSignSigner());
                        JgFileUtils.safeReplace(tempFile, outFile.toPath());
                        tempFile = null;
                    } catch (Exception e) {
                        System.err.println("transform failed: [" + arg + "]: " + e.getMessage());
                        return 1;
                    } finally {
                        if (tempFile != null) {
                            try {
                                Files.deleteIfExists(tempFile);
                            } catch (IOException ignored) {
                                tempFile.toFile().deleteOnExit();
                            }
                        }
                    }
                }
            }
        }

        if (appConfig.isGenLauncher() /*|| appConfig.isGenDevLauncher()*/) {
            try {
                LauncherCodeGenerator.generate(output, transformInfo);
            } catch (TransformException e) {
                System.err.println("ERROR: jg launcher generate failed: " + e.getMessage());
                e.printStackTrace();
                return 1;
            }
        }
        return 0;
    }

//    private static byte[] signFile(Path path, SignatureInfo signatureInfo) {
//        byte[] buf = new byte[4096];
//        try (InputStream inputStream = Files.newInputStream(path)) {
//            Signature signature = signatureInfo.getSignSignature();
//            int read;
//            while ((read = inputStream.read(buf)) != -1) {
//                signature.update(buf, 0, read);
//            }
//            return signature.sign();
//        } catch (Exception e) {
//            throw new Error("sign failed: [" + path + "]", e);
//        }
//    }

    private static CommandLine parseArgs(String[] args) throws ParseException {
        return new DefaultParser().parse(OPTIONS, args);
    }

    private static String message(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.trim().isEmpty()
                ? throwable.getClass().getSimpleName()
                : message;
    }

    private static void printUsage() {
        new HelpFormatter().printHelp("java-guard", OPTIONS);
    }

    private static AppConfig appArgs(CommandLine parse) {
        String configPath = parse.getOptionValue(CONFIG_OPTION.getOpt(), "./config.yml");
        String output = parse.getOptionValue(OUTPUT_OPTION);
        String mode = parse.getOptionValue(MODE_OPTION);
//        boolean devFeature = parse.hasOption(DEV_LAUNCHER_OPTION);
        AppConfig appConfig;
        try (FileInputStream inputStream = new FileInputStream(configPath)) {
            Yaml yaml = new Yaml();
            appConfig = yaml.loadAs(new InputStreamReader(inputStream, StandardCharsets.UTF_8), AppConfig.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read config file: " + configPath, e);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Malformed config file: " + configPath, e);
        }
        if (appConfig == null) {
            throw new IllegalArgumentException("Config file is empty: " + configPath);
        }
        TransformType transformType;
        if (mode != null) {
            try {
                transformType = TransformType.valueOf(mode);
            } catch (Exception e) {
                throw new IllegalArgumentException("not support mode: " + mode, e);
            }
            appConfig.setMode(transformType);
        }
        if (output != null) {
            appConfig.setOutput(output);
        }
//        appConfig.setGenDevLauncher(devFeature);
        appConfig.setSkipDeps(parse.hasOption(SKIP_DEPS_OPTION));
        appConfig.setGenLauncher(parse.hasOption(GENERATE_LAUNCHER_OPTION));
        return appConfig;
    }

    private static TransformInfo transformInfo(AppConfig config) {
        TransformInfo transformInfo = new TransformInfo();
        transformInfo.setConfig(config);
        if (config.getMatches() != null) {
            transformInfo.setMatches(config.getMatches());
        }
        if (config.getZipLevel() != null) {
            transformInfo.setLevel(config.getZipLevel());
        }
        String keyString = config.getKey();
        boolean isDecrypt = config.getMode() == TransformType.decrypt;
        if (keyString == null) {
            if (isDecrypt) {
                System.err.println("key required with decrypt mode");
                return null;
            } else {
                KeyGenerator generator = null;
                try {
                    generator = KeyGenerator.getInstance(ConstVars.ALGORITHM);
                } catch (NoSuchAlgorithmException e) {
                    System.err.println("generate key failed: " + e.getMessage());
                    return null;
                }
                SecretKey secretKey = generator.generateKey();
                keyString = Base64.encodeBase64URLSafeString(secretKey.getEncoded());
                System.out.println(">>> generate key: " + keyString);
            }
        }
        byte[] hmac = new HmacUtils(HmacAlgorithms.HMAC_SHA_512, ConstVars.SALT).hmac(keyString.getBytes(StandardCharsets.UTF_8));
//        encryptInfo.setKey(Arrays.copyOfRange(hmac, 0, 512 >> 4));
//        encryptInfo.setResourceKey(Arrays.copyOfRange(hmac, 512 >> 4, hmac.length));
        transformInfo.setKeyInfo(new KeyInfo(Arrays.copyOfRange(hmac, 0, 512 >> 4)));
        transformInfo.setResourceKeyInfo(new KeyInfo(Arrays.copyOfRange(hmac, 512 >> 4, hmac.length)));

        SignatureInfo signatureInfo = SignatureInfo.fromConfig(config);
        transformInfo.setSignature(signatureInfo);
        return transformInfo;
    }
}