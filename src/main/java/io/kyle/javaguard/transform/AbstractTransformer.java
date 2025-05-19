package io.kyle.javaguard.transform;

import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.exception.TransformException;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/08 10:32
 */
public abstract class AbstractTransformer implements Transformer {
    protected final TransformInfo transformInfo;

    public AbstractTransformer(TransformInfo transformInfo) {
        this.transformInfo = transformInfo;
    }

    protected void copyStream(InputStream in, OutputStream out) throws TransformException {
        try {
            IOUtils.copy(in, out);
        } catch (IOException e) {
            throw new TransformException(e);
        }
    }
}
