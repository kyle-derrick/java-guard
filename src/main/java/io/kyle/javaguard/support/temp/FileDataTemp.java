package io.kyle.javaguard.support.temp;

import io.kyle.javaguard.support.TransformDataOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2025/9/16 12:46
 */
public class FileDataTemp implements DataTemp {
    private final File tempFile;
    private final TransformDataOutputStream out;

    public FileDataTemp(File tempFile) throws IOException {
        this.tempFile = tempFile;
        this.out = new TransformDataOutputStream(Files.newOutputStream(tempFile.toPath()));
    }

    @Override
    public TransformDataOutputStream getOutputStream() {
        return out;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(tempFile.toPath());
    }
}
