package com.store.exception;

public class IncompatibilidadException extends RuntimeException {
    public IncompatibilidadException(String message) {
        super(message);
    }
    public IncompatibilidadException(String message, Throwable cause) {
        super(message, cause);
    }

}
