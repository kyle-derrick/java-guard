package io.kyle.javaguard.transform;

import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.exception.TransformException;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/9/30 14:17
 */
public class DefaultTransformer extends AbstractTransformer {

    public DefaultTransformer(TransformInfo encryptInfo) {
        super(encryptInfo);
    }

    @Override
    public boolean isSupport(String name) {
        return true;
    }

    @Override
    public boolean encrypt(InputStream in, OutputStream out) throws TransformException {
        // todo
        copyStream(in, out);
        return true;
    }

    @Override
    public boolean decrypt(InputStream in, OutputStream out) throws TransformException {
        // todo
        copyStream(in, out);
        return true;
    }
}