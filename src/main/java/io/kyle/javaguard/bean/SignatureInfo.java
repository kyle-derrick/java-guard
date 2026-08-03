package io.kyle.javaguard.bean;

import io.kyle.javaguard.constant.ConstVars;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil;
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/08 10:32
 */
public class SignatureInfo {
    private Ed25519PrivateKeyParameters privateKey;
    private Ed25519PublicKeyParameters publicKey;
    private Signer signature;

    public Signer getSignSignature() throws InvalidKeyException, NoSuchAlgorithmException {
        if (signature == null) {
            signature = newSignSigner();
        }
        return signature;
    }

    public static SignatureInfo fromConfig(AppConfig config) {
        String privateKeyPath = configuredPath(config.getPrivateKey(), ConstVars.DEFAULT_PRIVATE_KEY);
        if (privateKeyPath == null) {
            throw new IllegalArgumentException("signing requires an Ed25519 private key");
        }

        SignatureInfo signatureInfo = new SignatureInfo();
        signatureInfo.setPrivateKey(readPrivateKey(privateKeyPath));
        Ed25519PublicKeyParameters derivedPublicKey = signatureInfo.privateKey.generatePublicKey();

        String publicKeyPath = StringUtils.isBlank(config.getPublicKey()) ? null : config.getPublicKey();
        if (publicKeyPath != null) {
            Ed25519PublicKeyParameters configuredPublicKey = readPublicKey(publicKeyPath);
            if (!MessageDigest.isEqual(derivedPublicKey.getEncoded(), configuredPublicKey.getEncoded())) {
                throw new IllegalArgumentException("public key does not match the configured private key: " + publicKeyPath);
            }
        }
        signatureInfo.setPublicKey(derivedPublicKey);
        return signatureInfo;
    }

    private static String configuredPath(String configuredPath, String defaultPath) {
        if (StringUtils.isNotBlank(configuredPath)) {
            return configuredPath;
        }
        return Files.isRegularFile(Paths.get(defaultPath)) ? defaultPath : null;
    }

    private static Ed25519PrivateKeyParameters readPrivateKey(String path) {
        try (PemReader reader = new PemReader(new FileReader(path))) {
            PemObject pemObject = reader.readPemObject();
            if (pemObject == null) {
                throw new IllegalArgumentException("private key PEM is empty");
            }
            AsymmetricKeyParameter key = OpenSSHPrivateKeyUtil.parsePrivateKeyBlob(pemObject.getContent());
            if (!(key instanceof Ed25519PrivateKeyParameters)) {
                throw new IllegalArgumentException("private key is not an Ed25519 private key");
            }
            return (Ed25519PrivateKeyParameters) key;
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            throw new IllegalArgumentException("failed to read private key [" + path + "]", e);
        }
    }

    private static Ed25519PublicKeyParameters readPublicKey(String path) {
        try {
            String content = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8).trim();
            String[] fields = content.split("\\s+");
            if (fields.length < 2 || !"ssh-ed25519".equals(fields[0])) {
                throw new IllegalArgumentException("public key is not in OpenSSH Ed25519 format");
            }
            byte[] encoded = Base64.decodeBase64(fields[1]);
            if (encoded.length == 0 || !Base64.isBase64(fields[1])) {
                throw new IllegalArgumentException("public key contains invalid base64");
            }
            AsymmetricKeyParameter key = OpenSSHPublicKeyUtil.parsePublicKey(encoded);
            if (!(key instanceof Ed25519PublicKeyParameters)) {
                throw new IllegalArgumentException("public key is not an Ed25519 public key");
            }
            return (Ed25519PublicKeyParameters) key;
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            throw new IllegalArgumentException("failed to read public key [" + path + "]", e);
        }
    }

    public String getKeyHash() {
        if (publicKey == null) {
            return "-";
        }
        return new HmacUtils(HmacAlgorithms.HMAC_MD5, ConstVars.SALT)
                .hmacHex(publicKey.getEncoded());
    }

    public Signer newSignSigner() throws InvalidKeyException, NoSuchAlgorithmException {
        if (privateKey == null) {
            throw new InvalidKeyException("signing requires an Ed25519 private key");
        }
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, privateKey);
        return signer;
    }

    public Signer newVerifySigner() throws InvalidKeyException, NoSuchAlgorithmException {
        if (publicKey == null) {
            throw new InvalidKeyException("verification requires an Ed25519 public key");
        }
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(false, publicKey);
        return signer;
    }

    public Ed25519PrivateKeyParameters getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(Ed25519PrivateKeyParameters privateKey) {
        this.privateKey = privateKey;
        this.signature = null;
    }

    public Ed25519PublicKeyParameters getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(Ed25519PublicKeyParameters publicKey) {
        this.publicKey = publicKey;
    }
}
