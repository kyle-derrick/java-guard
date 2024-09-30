package io.kyle.javaguard.translate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/9/30 16:41
 */
public interface Transformer {
    void transform(final InputStream in, final OutputStream out) throws IOException;
}
