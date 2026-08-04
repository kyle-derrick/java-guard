package io.kyle.javaguard.compat.language;
import java.util.concurrent.atomic.AtomicReference;
public final class LanguageFeatures {
    public static boolean check() throws InterruptedException {
        AtomicReference<String> value = new AtomicReference<>();
        Thread thread = Thread.ofVirtual().name("compat-virtual").start(() -> value.set("jdk21"));
        thread.join();
        return thread.isVirtual() && "jdk21".equals(value.get());
    }
    private LanguageFeatures() { }
}
