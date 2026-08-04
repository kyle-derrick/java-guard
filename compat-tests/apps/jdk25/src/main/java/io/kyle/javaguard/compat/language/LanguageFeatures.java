package io.kyle.javaguard.compat.language;
import java.util.List;
import java.util.stream.Gatherers;
import java.util.stream.Stream;
public final class LanguageFeatures {
    public static boolean check() {
        List<List<Integer>> windows = Stream.of(1, 2, 3).gather(Gatherers.windowFixed(2)).toList();
        return windows.equals(List.of(List.of(1, 2), List.of(3)));
    }
    private LanguageFeatures() { }
}
