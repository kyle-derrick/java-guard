package io.kyle.javaguard.filter;

import org.apache.commons.io.FilenameUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DefaultFilter implements Filter {
    public static final DefaultFilter INSTANCE = new DefaultFilter();
    private static final Set<String> DEFAULT_TRANSFORM_EXTENSIONS = new HashSet<>(Arrays.asList(
            "class",
            "jar",
            "json",
            "xml",
            "properties",
            "yml",
            "conf",
            "config",
            "html",
            "css",
            "js"
        ));

    private DefaultFilter() {
    }

    @Override
    public boolean filtrate(String path) {
        return FilenameUtils.isExtension(path, DEFAULT_TRANSFORM_EXTENSIONS);
    }
}
