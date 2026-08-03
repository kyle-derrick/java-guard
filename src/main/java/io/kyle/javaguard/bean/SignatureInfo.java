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
import org.bouncycastle.util.io.pem.PemReader;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.*;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/08 10:32
 */
public class SignatureInfo {
    private Ed25519PrivateKeyParameters privateKey;
    private Ed25519PublicKeyParameters publicKey;
    private Signer signature;

    public Signer getSignSignature() throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException {
        if (signature == null) {
            signature = newSignSigner();
        }
        return signature;
    }

    public static SignatureInfo fromConfig(AppConfig config) {
        SignatureInfo signatureInfo = new SignatureInfo();
        String privateKeyPath = config.getPrivateKey();
        // todo 只有私钥或者没有指定时，生成
        if (StringUtils.isBlank(privateKeyPath)) {
            if (Files.exists(Paths.get(ConstVars.DEFAULT_PRIVATE_KEY))) {
                privateKeyPath = ConstVars.DEFAULT_PRIVATE_KEY;
            } else {
                privateKeyPath = null;
            }
        }
        if (privateKeyPath != null) {
            try (PemReader privateKeyReader = new PemReader(new FileReader(privateKeyPath))) {
                AsymmetricKeyParameter keyParameter = OpenSSHPrivateKeyUtil.parsePrivateKeyBlob(privateKeyReader.readPemObject().getContent());
                if (!(keyParameter instanceof Ed25519PrivateKeyParameters)) {
                    throw new Error("the private key is not Ed25519 private key!");
                }
                signatureInfo.setPrivateKey((Ed25519PrivateKeyParameters) keyParameter);
            } catch (Exception e) {
                System.err.println("Failed to read private key: [" + privateKeyPath + "]: " + e.getMessage());
            }
        }
        String publicKeyPath = config.getPublicKey();
        // todo 只有私钥或者没有指定时，生成
        if (StringUtils.isBlank(publicKeyPath)) {
            if (Files.exists(Paths.get(ConstVars.DEFAULT_PUBLIC_KEY))) {
                publicKeyPath = ConstVars.DEFAULT_PUBLIC_KEY;
            } else {
                publicKeyPath = null;
            }
        }
        if (publicKeyPath != null) {
            try {
                String content = FileUtils.readFileToString(new File(publicKeyPath), StandardCharsets.UTF_8);
                String[] split = content.split(StringUtils.SPACE);
                if (split.length > 2) {
                    AsymmetricKeyParameter keyParameter = OpenSSHPublicKeyUtil.parsePublicKey(Base64.decodeBase64(split[1]));
                    if (!(keyParameter instanceof Ed25519PublicKeyParameters)) {
                        throw new Error("the public key is not Ed25519 public key!");
                    }

//                    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.decodeBase64(split[1]));
//                    PublicKey publicKey = KeyFactory.getInstance(ConstVars.SIGN_ALGORITHM).generatePublic(keySpec);
                    signatureInfo.setPublicKey((Ed25519PublicKeyParameters) keyParameter);
                } else {
                    System.out.println("WARN: public key parse failed: " + content);
                }
            } catch (Exception e) {
                System.err.println("Failed to read public key: [" + publicKeyPath + "]: " + e.getMessage());
            }
        }

        if (signatureInfo.publicKey == null && signatureInfo.privateKey == null) {
            return null;
        }
        if (signatureInfo.publicKey == null) {
            try {
//                PublicKey publicKey = KeyFactory.getInstance(ConstVars.SIGN_ALGORITHM)
//                        .generatePublic(new PKCS8EncodedKeySpec(signatureInfo.privateKey.getEncoded()));
                signatureInfo.setPublicKey(signatureInfo.privateKey.generatePublicKey());
            } catch (Exception e) {
                throw new Error("cannot generate public key from private key!", e);
            }
        }
        return signatureInfo;
    }

    public String getKeyHash() {
        if (publicKey == null) {
            return "-";
        }
        return new HmacUtils(HmacAlgorithms.HMAC_MD5, ConstVars.SALT)
                .hmacHex(publicKey.getEncoded());
    }

    public Signer newSignSigner() throws InvalidKeyException, NoSuchAlgorithmException {
        Ed25519PrivateKeyParameters privateKey = getPrivateKey();
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, privateKey);
//        Signature signature = Signature.getInstance(ConstVars.SIGN_ALGORITHM);
//        signature.initSign(privateKey);
        return signer;
    }

    public Signer newVerifySigner() throws InvalidKeyException, NoSuchAlgorithmException {
        Ed25519PublicKeyParameters publicKey = getPublicKey();
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(false, publicKey);
//        Signature signature = Signature.getInstance(ConstVars.SIGN_ALGORITHM);
//        signature.initVerify(publicKey);
        return signer;
    }

    public Ed25519PrivateKeyParameters getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(Ed25519PrivateKeyParameters privateKey) {
        this.privateKey = privateKey;
    }

    public Ed25519PublicKeyParameters getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(Ed25519PublicKeyParameters publicKey) {
        this.publicKey = publicKey;
    }
}
