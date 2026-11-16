package com.store.exception;

public class EstadoPedidoInvalidoException extends NegocioException {
    public EstadoPedidoInvalidoException(String message) {
        super(message);
    }
    public EstadoPedidoInvalidoException(String message, Throwable cause) {
        super(message, cause);
    }

}
