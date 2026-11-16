package com.store.exception;

public class PedidoNoCancelableException extends RuntimeException {
    public PedidoNoCancelableException(String message) {
        super(message);
    }
    public PedidoNoCancelableException(String message, Throwable cause) {
        super(message, cause);
    }

}
