package io.kyle.javaguard.compat.language;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
public final class LanguageFeatures {
    public static boolean check() { return "amRrOA==".equals(Base64.getEncoder().encodeToString("jdk8".getBytes(StandardCharsets.UTF_8))); }
    private LanguageFeatures() { }
}
