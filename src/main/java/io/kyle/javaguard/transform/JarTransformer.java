package io.kyle.javaguard.transform;

import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.constant.ConstVars;
import io.kyle.javaguard.constant.TransformType;
import io.kyle.javaguard.filter.DefaultFilter;
import io.kyle.javaguard.filter.Filter;
import io.kyle.javaguard.filter.SimpleFilter;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/9/30 10:44
 */
public class JarTransformer extends AbstractTransformer {
    private final Filter filter;
    private final Transformer[] transformers;

    public JarTransformer(TransformInfo transformInfo) {
        super(transformInfo);

        Filter filter = DefaultFilter.INSTANCE;
        if (transformInfo.getMatches() != null) {
            SimpleFilter simpleFilter = new SimpleFilter();
            for (String match : transformInfo.getMatches()) {
                simpleFilter.addExpr(match);
            }
            filter = simpleFilter;
        }
        this.filter = filter;

        transformers = new Transformer[Transformer.encryption.length];
        for (int i = 0; i < Transformer.encryption.length; i++) {
            Transformer.TransformCreator transformCreator = Transformer.encryption[i];
            transformers[i] = transformCreator.create(transformInfo);
        }
    }

    @Override
    public boolean isSupport(String name) {
        return StringUtils.endsWith(name, ".jar");
    }

    @Override
    public boolean encrypt(InputStream in, OutputStream out) throws IOException {
        transform(in, out, true);
        return true;
    }

    @Override
    public boolean decrypt(InputStream in, OutputStream out) throws IOException {
        transform(in, out, false);
        return true;
    }

    public void transform(final InputStream in, final OutputStream out, boolean encrypt) throws IOException {
        JarArchiveInputStream zis = new JarArchiveInputStream(in);
        JarArchiveOutputStream zos = new JarArchiveOutputStream(out);
        zos.setLevel(transformInfo.getLevel());
        JarArchiveEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            zos.putArchiveEntry((ZipArchiveEntry) entry.clone());
            boolean directory = entry.isDirectory();
            if (directory) {
                zos.closeArchiveEntry();
                continue;
            }
            boolean transformed = false;
            if (!entry.getName().equals(ConstVars.META_INF_MANIFEST)
                    && filter.filtrate(entry.getName())) {
                for (Transformer transformer : transformers) {
                    if (transformer.isSupport(entry.getName())) {
                        if (encrypt) {
                            transformed = transformer.encrypt(zis, zos);
                        } else {
                            transformed = transformer.decrypt(zis, zos);
                        }
                        if (transformed) {
                            break;
                        }
                    }
                }
            }
            if (!transformed) {
                IOUtils.copy(zis, zos);
            }
            zos.closeArchiveEntry();
        }

        zos.finish();
    }
}
