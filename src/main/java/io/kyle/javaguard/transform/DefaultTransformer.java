package io.kyle.javaguard.transform;

import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.constant.ConstVars;
import io.kyle.javaguard.exception.TransformException;
import io.kyle.javaguard.support.JGTransformInputStream;

import javax.crypto.Cipher;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

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
        try {
            out.write(ConstVars.ENCRYPT_RESOURCE_HEADER);
            JGTransformInputStream transformInputStream = new JGTransformInputStream(in, encryptInfo.getResourceCipher(Cipher.ENCRYPT_MODE));
            copyStream(transformInputStream, out);
        } catch (Exception e) {
            throw new TransformException("resource encrypt failed", e);
        }
        return true;
    }

    @Override
    public boolean decrypt(InputStream in, OutputStream out) throws TransformException {
        byte[] header = new byte[ConstVars.ENCRYPT_RESOURCE_HEADER.length];
        try {
            in.read(header);
            if (!Objects.deepEquals(header, ConstVars.ENCRYPT_RESOURCE_HEADER)) {
                out.write(header);
                copyStream(in, out);
                return true;
            }
            JGTransformInputStream transformInputStream = new JGTransformInputStream(in, encryptInfo.getResourceCipher(Cipher.DECRYPT_MODE));
            copyStream(transformInputStream, out);
        } catch (Exception e) {
            throw new TransformException("decrypt resource failed", e);
        }
        return true;
    }
}