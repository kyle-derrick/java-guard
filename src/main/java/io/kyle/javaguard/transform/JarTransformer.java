package io.kyle.javaguard.transform;

import io.kyle.javaguard.bean.AppConfig;
import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.constant.ConstVars;
import io.kyle.javaguard.exception.TransformException;
import io.kyle.javaguard.filter.DefaultFilter;
import io.kyle.javaguard.filter.Filter;
import io.kyle.javaguard.filter.SimpleFilter;
import io.kyle.javaguard.support.TransformDataOutputStream;
import io.kyle.javaguard.support.temp.BuffDataTemp;
import io.kyle.javaguard.support.temp.DataTemp;
import io.kyle.javaguard.support.temp.FileDataTemp;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

import java.io.*;
import java.nio.file.Files;

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

        transformers = new Transformer[Transformer.TRANSFORMERS.length];
        for (int i = 0; i < Transformer.TRANSFORMERS.length; i++) {
            TransformerCreator transformerCreator = Transformer.TRANSFORMERS[i];
            if (transformerCreator == Transformer.JAR_TRANSFORMER) {
                transformers[i] = this;
            } else {
                transformers[i] = transformerCreator.create(transformInfo);
            }
        }
    }

    @Override
    public boolean isSupport(String name) {
        return name.endsWith(".jar");
    }

    @Override
    public boolean encrypt(InputStream in, OutputStream out) throws TransformException {
        try {
            transform(in, out, true);
        } catch (IOException e) {
            throw new TransformException(e);
        }
        return true;
    }

    @Override
    public boolean decrypt(InputStream in, OutputStream out) throws TransformException {
        try {
            transform(in, out, false);
        } catch (IOException e) {
            throw new TransformException(e);
        }
        return true;
    }

    protected void transform(final InputStream in, final OutputStream out, boolean encrypt) throws IOException, TransformException {
        JarArchiveInputStream zis = new JarArchiveInputStream(in);
        JarArchiveOutputStream zos = new JarArchiveOutputStream(out);
        BufferedInputStream buffZis = new BufferedInputStream(zis, 2048);
        zos.setLevel(transformInfo.getLevel());
        JarArchiveEntry entry;
        AppConfig config = transformInfo.getConfig();
        File tempFile = null;
        try {
            while ((entry = zis.getNextEntry()) != null) {
                boolean directory = entry.isDirectory();
                ZipArchiveEntry newEntry = (ZipArchiveEntry) entry.clone();
                if (directory) {
                    zos.putArchiveEntry(newEntry);
                    zos.closeArchiveEntry();
                    continue;
                }
                boolean transformed = false;
                DataTemp dataTemp = null;
                boolean needEncrypt = !entry.getName().equals(ConstVars.META_INF_MANIFEST)
                        && filter.filtrate(entry.getName());
                if (needEncrypt) {
                    if (config.isPrintEncryptEntry()) {
                        System.out.println("INFO: try encrypt entry: " + entry.getName());
                    }
                    for (Transformer transformer : transformers) {
                        if (transformer.isSupport(entry.getName())) {
                            long uncompressedSize = entry.getSize();
                            if (uncompressedSize < 0) {
                                uncompressedSize = entry.getCompressedSize() << 1;
                            }
                            if (uncompressedSize < 0 || uncompressedSize > config.getBufferSize()) {
                                if (tempFile == null) {
                                    tempFile = Files.createTempFile(ConstVars.TEMP_PATH_PREFIX, ".temp").toFile();
                                }
                                dataTemp = new FileDataTemp(tempFile);
                            } else {
                                dataTemp = new BuffDataTemp((int) uncompressedSize);
                            }
                            try (TransformDataOutputStream outputStream = dataTemp.getOutputStream()) {
                                if (encrypt) {
                                    transformed = transformer.encrypt(buffZis, outputStream);
                                } else {
                                    transformed = transformer.decrypt(buffZis, outputStream);
                                }
                                if (transformed) {
                                    newEntry.setCrc(outputStream.getCrc());
                                    newEntry.setSize(outputStream.getSize());
                                    break;
                                }
                            }
                        }
                    }
                }
                zos.putArchiveEntry(newEntry);
                if (transformed) {
                    try (InputStream inputStream = dataTemp.getInputStream()) {
                        copyStream(inputStream, zos);
                    }
                } else {
                    copyStream(zis, zos);
                }

                zos.closeArchiveEntry();
                if (needEncrypt && config.isPrintEncryptEntry()) {
                    System.out.println("INFO: encrypt entry " + (transformed ? "successful" : "failed") + ": " + entry.getName());
                }
            }
            zos.flush();
            zos.finish();
        } finally {
//            if (tempFile != null) {
//                try {
//                    FileUtils.forceDelete(tempFile);
//                } catch (Exception e) {
//                    FileUtils.forceDeleteOnExit(tempFile);
//                }
//            }
        }

    }
}
