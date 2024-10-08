package io.kyle.javaguard.exception;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/08 10:32
 */
public class TransformException extends Exception {
    public TransformException() {
    }

    public TransformException(String message) {
        super(message);
    }

    public TransformException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransformException(Throwable cause) {
        super(cause);
    }

    public TransformException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
