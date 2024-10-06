package io.kyle.javaguard.exception;

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
