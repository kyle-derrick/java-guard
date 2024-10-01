package io.kyle.javaguard.transform;

import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.constant.ConstVars;
import io.kyle.javaguard.filter.Filter;
import io.kyle.javaguard.filter.SimpleFilter;
import io.kyle.javaguard.transform.encryption.Encryption;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/9/30 10:44
 */
public class PackTransformer {
    private final TransformInfo config;
    private final Filter filter;
    private final Encryption[] encryptions;

    public PackTransformer(TransformInfo config) {
        this.config = config;
        SimpleFilter simpleFilter = new SimpleFilter();
        for (String match : config.getMatches()) {
            simpleFilter.addExpr(match);
        }
        filter = simpleFilter;

        encryptions = new Encryption[Encryption.encryption.length];
        for (int i = 0; i < Encryption.encryption.length; i++) {
            Encryption.EncryptionCreator encryptionCreator = Encryption.encryption[i];
            encryptions[i] = encryptionCreator.create(config.getEncrypt());
        }
    }

    public void transform(final InputStream in, final OutputStream out) throws IOException {
        JarArchiveInputStream zis = new JarArchiveInputStream(in);
        JarArchiveOutputStream zos = new JarArchiveOutputStream(out);
        zos.setLevel(config.getLevel());
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
                for (Encryption encryption : encryptions) {
                    if (encryption.isSupport(entry.getName())) {
                        encryption.encrypt(zis, zos);
                        transformed = true;
                        break;
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
