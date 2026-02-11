package io.kyle.javaguard.transform;

import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.constant.ConstVars;
import io.kyle.javaguard.exception.TransformException;
import io.kyle.javaguard.support.StandardResourceInputStream;

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
            in.mark(ConstVars.ENCRYPT_RESOURCE_HEADER.length);
            boolean encrypted = true;
            for (byte b : ConstVars.ENCRYPT_RESOURCE_HEADER) {
                if (in.read() != b) {
                    encrypted = false;
                }
            }
            in.reset();
            if (encrypted) {
                out.write(ConstVars.ENCRYPT_RESOURCE_HEADER);
                copyStream(in, out);
                return true;
            }
            out.write(ConstVars.ENCRYPT_RESOURCE_HEADER);
            StandardResourceInputStream transformInputStream = new StandardResourceInputStream(in, transformInfo.getResourceKeyInfo(), true);
            copyStream(transformInputStream, out);
        } catch (TransformException e) {
            throw e;
        } catch (Exception e) {
            throw new TransformException("resource encrypt failed", e);
        }
        return true;
    }

    @Override
    public boolean decrypt(InputStream in, OutputStream out) throws TransformException {
        byte[] header = new byte[ConstVars.ENCRYPT_RESOURCE_HEADER.length];
        try {
            int read = in.read(header);
            if (read < header.length) {
                out.write(header, 0, header.length);
                return true;
            }
            if (!Objects.deepEquals(header, ConstVars.ENCRYPT_RESOURCE_HEADER)) {
                out.write(header);
                copyStream(in, out);
                return true;
            }
            StandardResourceInputStream transformInputStream = new StandardResourceInputStream(in, transformInfo.getResourceKeyInfo(), false);
            copyStream(transformInputStream, out);
        } catch (TransformException e) {
            throw e;
        } catch (Exception e) {
            throw new TransformException("decrypt resource failed", e);
        }
        return true;
    }
}