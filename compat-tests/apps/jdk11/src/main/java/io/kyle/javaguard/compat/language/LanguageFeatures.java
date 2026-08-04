package io.kyle.javaguard.compat.language;
public final class LanguageFeatures {
    public static boolean check() { return "jdk11jdk11".equals("jdk11".repeat(2)) && " ".isBlank(); }
    private LanguageFeatures() { }
}
