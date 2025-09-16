package io.kyle.javaguard.support.temp;

import io.kyle.javaguard.support.TransformDataOutputStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2025/9/16 12:46
 */
public interface DataTemp {
    TransformDataOutputStream getOutputStream();
    InputStream getInputStream() throws IOException;
}
