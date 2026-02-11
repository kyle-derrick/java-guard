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
    private static final Option HELP_OPTION =
            new Option("h", "help", false, "print usage");
    private static final Options OPTIONS = new Options()
            .addOption(CONFIG_OPTION)
            .addOption(MODE_OPTION)
            .addOption(OUTPUT_OPTION)
            .addOption(GENERATE_LAUNCHER_OPTION)
            .addOption(HELP_OPTION);

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) {
        CommandLine parse = parseArgs(args);
        if (parse == null) {
            return;
        }
        AppConfig appConfig = appArgs(parse);
        String output = appConfig.getOutput();
        boolean isDecrypt = appConfig.getMode() == TransformType.decrypt;
        TransformInfo transformInfo = transformInfo(appConfig);
        if (transformInfo == null) {
            return;
        }
        File outputFile = new File(output);
        if (outputFile.exists() && outputFile.isFile()) {
            System.err.println("output dir is exists file: " + output);
            return;
        }
        if (!outputFile.exists()) {
            outputFile.mkdirs();
        }
        SignatureInfo signatureInfo = transformInfo.getSignature();
        String[] jars = parse.getArgs();
        boolean generateLauncher = GENERATE_LAUNCHER_OPTION.hasArg();
        if (ArrayUtils.isNotEmpty(jars)) {
            for (String arg : jars) {
                if (arg.endsWith(".jar")) {
                    File outFile = new File(outputFile, FilenameUtils.getName(arg));
                    try (FileInputStream in = new FileInputStream(arg);
                         FileOutputStream out = new FileOutputStream(outFile)) {
                        JarTransformer jarTransformer = new JarTransformer(transformInfo);
                        if (isDecrypt) {
                            jarTransformer.decrypt(in, out);
                        } else if (TransformType.signature != appConfig.getMode()) {
                            jarTransformer.encrypt(in, out);
                        } else {
                            IOUtils.copy(in, out);
                        }
                        out.flush();
                        ZipSignUtils.sign(outFile, signatureInfo.getSignSignature());
//                    byte[] sign = signFile(outFile.toPath(), signatureInfo);
//                    FileUtils.writeByteArrayToFile(new File(outFile.getAbsolutePath() + ".sign"), sign);
                    } catch (Exception e) {
                        throw new Error("transform failed: [" + arg + "]", e);
                    }
                }
            }
        }

        if (generateLauncher) {
            try {
                LauncherCodeGenerator.generate(output, transformInfo);
            } catch (TransformException e) {
                System.err.println("ERROR: jd launcher generate failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
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

    private static CommandLine parseArgs(String[] args) {
        DefaultParser defaultParser = new DefaultParser();
        try {
            CommandLine parse = defaultParser.parse(OPTIONS, args);
            if (!parse.hasOption(HELP_OPTION)) {
                return parse;
            }
        } catch (ParseException ignored) {
        }
        printUsage();
        return null;
    }

    private static void printUsage() {
        new HelpFormatter().printHelp("java-guard", OPTIONS);
        System.exit(0);
    }

    private static AppConfig appArgs(CommandLine parse) {
        String configPath = parse.getOptionValue(CONFIG_OPTION.getOpt(), "./config.yml");
        String output = parse.getOptionValue(OUTPUT_OPTION);
        String mode = parse.getOptionValue(MODE_OPTION);
        AppConfig appConfig;
        try (FileInputStream inputStream = new FileInputStream(configPath)) {
            Yaml yaml = new Yaml();
            appConfig = yaml.loadAs(new InputStreamReader(inputStream, StandardCharsets.UTF_8), AppConfig.class);
        } catch (IOException e) {
            throw new Error("Failed to read config file: " + configPath, e);
        }
        TransformType transformType;
        if (mode != null) {
            try {
                transformType = TransformType.valueOf(mode);
            } catch (Exception e) {
                throw new Error("not support mode: " + mode);
            }
            appConfig.setMode(transformType);
        }
        if (output != null) {
            appConfig.setOutput(output);
        }
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
        if (signatureInfo == null) {
            System.err.println("not found sign private key and public key");
            System.exit(-1);
        }
        transformInfo.setSignature(signatureInfo);
        return transformInfo;
    }
}