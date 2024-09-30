package io.kyle.javaguard.translate;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/9/30 14:17
 */
public class NopTransformer implements Transformer {
    public static final NopTransformer INSTANCE = new NopTransformer();

    private NopTransformer() {
    }

    @Override
    public void transform(InputStream in, OutputStream out) throws IOException {
        IOUtils.copy(in, out);
    }
}