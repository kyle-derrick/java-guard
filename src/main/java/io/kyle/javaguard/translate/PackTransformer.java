package io.kyle.javaguard.translate;

import io.kyle.javaguard.constant.ConstVars;
import io.kyle.javaguard.filter.Filter;
import io.kyle.javaguard.filter.SimpleFilter;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/9/30 10:44
 */
public class PackTransformer implements Transformer {
    private final TranslatorConfig config;
    private final Filter filter;
    private final Transformer transformer;

    public PackTransformer(TranslatorConfig config, Transformer transformer) {
        this.config = config;
        this.transformer = transformer;
        SimpleFilter simpleFilter = new SimpleFilter();
        for (String match : config.getMatches()) {
            simpleFilter.addExpr(match);
        }
        filter = simpleFilter;

    }

    @Override
    public void transform(final InputStream in, final OutputStream out) throws IOException {
        JarArchiveInputStream zis = new JarArchiveInputStream(in);
        JarArchiveOutputStream zos = new JarArchiveOutputStream(out);
        zos.setLevel(config.getLevel());
        JarArchiveEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            Transformer transformer = NopTransformer.INSTANCE;
            zos.putArchiveEntry((ZipArchiveEntry) entry.clone());
            boolean directory = entry.isDirectory();
            if (!directory && !entry.getName().equals(ConstVars.META_INF_MANIFEST)
                    && filter.filtrate(entry.getName())) {
                transformer = this.transformer;
            }
            if (!directory) {
                transformer.transform(zis, zos);
            }
            zos.closeArchiveEntry();
        }

        zos.finish();
    }
}
