package io.kyle.javaguard.transform;

import io.kyle.javaguard.bean.TransformInfo;
import io.kyle.javaguard.constant.ConstVars;
import io.kyle.javaguard.exception.TransformException;
import io.kyle.javaguard.support.StandardResourceInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;

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
            byte[] header = new byte[ConstVars.ENCRYPT_RESOURCE_HEADER.length];
            int headerLength = readHeader(in, header);
            InputStream resource = new SequenceInputStream(
                    new ByteArrayInputStream(header, 0, headerLength), in);
            if (headerLength == header.length && Arrays.equals(header, ConstVars.ENCRYPT_RESOURCE_HEADER)) {
                copyStream(resource, out);
                return true;
            }
            out.write(ConstVars.ENCRYPT_RESOURCE_HEADER);
            StandardResourceInputStream transformInputStream = new StandardResourceInputStream(resource, transformInfo.getResourceKeyInfo(), true);
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
            int read = readHeader(in, header);
            if (read < header.length) {
                out.write(header, 0, read);
                return true;
            }
            if (!Arrays.equals(header, ConstVars.ENCRYPT_RESOURCE_HEADER)) {
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

    private static int readHeader(InputStream in, byte[] header) throws IOException {
        int totalRead = 0;
        while (totalRead < header.length) {
            int read = in.read(header, totalRead, header.length - totalRead);
            if (read == -1) {
                break;
            }
            if (read == 0) {
                int value = in.read();
                if (value == -1) {
                    break;
                }
                header[totalRead++] = (byte) value;
            } else {
                totalRead += read;
            }
        }
        return totalRead;
    }
}
