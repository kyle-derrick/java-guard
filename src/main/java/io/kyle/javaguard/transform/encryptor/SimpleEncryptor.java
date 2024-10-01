//package io.kyle.javaguard.translate.encryptor;
//
//import io.kyle.javaguard.bean.TranslatorConfig;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//
//public class SimpleEncryptor implements Encryptor {
//
//    public SimpleEncryptor(TranslatorConfig config) {
////        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
////        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");//"算法/模式/补码方式"
////        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
////        byte[] encrypted = cipher.doFinal(sSrc.getBytes("utf-8"));
////        return new BASE64Encoder().encode(encrypted);//此处使用BASE64做转码功能，同时能起到2次加密的作用。
//    }
//
//    @Override
//    public void encrypt(InputStream in, OutputStream out) throws IOException {
//
//    }
//
//    @Override
//    public void decrypt(InputStream in, OutputStream out) throws IOException {
//
//    }
//}
