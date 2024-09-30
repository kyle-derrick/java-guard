package io.kyle.javaguard.translate;

import java.util.zip.Deflater;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/9/30 10:46
 */
public class TranslatorConfig {
    private String[] matches;
    private int level = Deflater.DEFAULT_COMPRESSION;

    public String[] getMatches() {
        return matches;
    }

    public void setMatches(String[] matches) {
        this.matches = matches;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
