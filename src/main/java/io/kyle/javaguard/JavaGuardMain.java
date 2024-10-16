package io.kyle.javaguard;

import io.kyle.javaguard.bean.AppConfig;
import io.kyle.javaguard.bean.EncryptInfo;
import io.kyle.javaguard.bean.SignatureInfo;
import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.constant.ConstVars;
import io.kyle.javaguard.constant.TransformType;
import io.kyle.javaguard.support.LauncherCodeGenerator;
import io.kyle.javaguard.transform.JarTransformer;
import org.apache.commons.cli.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemReader;
import org.yaml.snakeyaml.Yaml;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.Signature;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/08 10:32
 */
public class JavaGuardMain {
    private static final Option CONFIG_OPTION =
            new Option("c", "config", true, "config files (default: ./config.yml)");
    private static final Option MODE_OPTION =
            new Option("mode", "mode", true, "encrypt/decrypt mode (default encrypt)");
    private static final Option OUTPUT_OPTION =
            new Option("o", "output", true, "output dir");
    private static final Option HELP_OPTION =
            new Option("h", "help", false, "print usage");
    private static final Options OPTIONS = new Options()
            .addOption(CONFIG_OPTION)
            .addOption(MODE_OPTION)
            .addOption(OUTPUT_OPTION)
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
        if (ArrayUtils.isEmpty(jars)) {
            printUsage();
        }
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
                        LauncherCodeGenerator.generate(output, transformInfo);
                    } else {
                        IOUtils.copy(in, out);
                    }
                    out.flush();
                    byte[] sign = signFile(outFile.toPath(), signatureInfo);
                    FileUtils.writeByteArrayToFile(new File(outFile.getAbsolutePath() + ".sign"), sign);
                } catch (Exception e) {
                    throw new Error("transform failed: [" + arg + "]", e);
                }
            }
        }
    }

    private static byte[] signFile(Path path, SignatureInfo signatureInfo) {
        byte[] buf = new byte[4096];
        try (InputStream inputStream = Files.newInputStream(path)) {
            Signature signature = signatureInfo.getSignature();
            int read;
            while ((read = inputStream.read(buf)) != -1) {
                signature.update(buf, 0, read);
            }
            return signature.sign();
        } catch (Exception e) {
            throw new Error("sign failed: [" + path + "]", e);
        }
    }

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
        new HelpFormatter().printHelp("JavaGuard", OPTIONS);
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
        if (config.getMatches() != null) {
            transformInfo.setMatches(config.getMatches());
        }
        if (config.getZipLevel() != null) {
            transformInfo.setLevel(config.getZipLevel());
        }
        EncryptInfo encryptInfo = new EncryptInfo();
        String keyString = config.getKey();
        boolean isDecrypt = config.getMode() == TransformType.decrypt;
        if (keyString == null) {
            if (isDecrypt) {
                System.err.println("key required with decrypt mode");
                return null;
            } else {
                KeyGenerator generator = null;
                try {
                    generator = KeyGenerator.getInstance(encryptInfo.getAlgorithm());
                } catch (NoSuchAlgorithmException e) {
                    System.err.println("generate key failed: " + e.getMessage());
                    return null;
                }
                SecretKey secretKey = generator.generateKey();
                keyString = Base64.encodeBase64URLSafeString(secretKey.getEncoded());
                System.out.println(">>> generate key: " + keyString);
            }
        }
        encryptInfo.setKey(DigestUtils.sha256(keyString));
        transformInfo.setEncrypt(encryptInfo);

        SignatureInfo signatureInfo = signatureInfo(config);
        transformInfo.setSignature(signatureInfo);
        return transformInfo;
    }

    private static SignatureInfo signatureInfo(AppConfig config) {
        String privateKey = config.getPrivateKey();
        String publicKey = config.getPublicKey();
        // todo 只有私钥或者没有指定时，生成
        privateKey = privateKey == null ? ConstVars.DEFAULT_PRIVATE_KEY : privateKey;
        publicKey = publicKey == null ? ConstVars.DEFAULT_PUBLIC_KEY : publicKey;
        SignatureInfo signatureInfo = new SignatureInfo();
        try (PemReader privateKeyReader = new PemReader(new FileReader(privateKey));
             PemReader publicKeyReader = new PemReader(new FileReader(privateKey))) {
            signatureInfo.setPrivateKey(privateKeyReader.readPemObject().getContent());
            signatureInfo.setPublicKey(publicKeyReader.readPemObject().getContent());
        } catch (Exception e) {
            throw new Error("Failed to read private/public key: [" +privateKey+ "]:["+publicKey+"]: " + e.getMessage());
        }
        return signatureInfo;
    }
}