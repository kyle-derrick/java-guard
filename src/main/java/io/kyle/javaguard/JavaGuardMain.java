package io.kyle.javaguard;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class JavaGuardMain {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        byte[] raw = "asdasdasdasdasda".getBytes(StandardCharsets.UTF_8);
        try {
            long s = System.currentTimeMillis();
            for (int i = 0; i < 10000; i++) {
                SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");//"算法/模式/补码方式"
                cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            }
            long e = System.currentTimeMillis() - s;
            System.out.println(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}