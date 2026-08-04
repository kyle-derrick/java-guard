package io.kyle.javaguard.compat.language;
public final class LanguageFeatures {
    sealed interface Value permits Text { }
    record Text(String value) implements Value { }
    public static boolean check() { Value value = new Text("jdk17"); return value instanceof Text text && "jdk17".equals(text.value()); }
    private LanguageFeatures() { }
}
