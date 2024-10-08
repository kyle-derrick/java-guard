package io.kyle.javaguard.exception;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/08 10:32
 */
public class TransformRuntimeException extends RuntimeException {
    public TransformRuntimeException() {
    }

    public TransformRuntimeException(String message) {
        super(message);
    }

    public TransformRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransformRuntimeException(Throwable cause) {
        super(cause);
    }

    public TransformRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
