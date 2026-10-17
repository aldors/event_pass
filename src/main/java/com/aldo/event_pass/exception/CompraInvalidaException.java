package com.aldo.event_pass.exception;

public class CompraInvalidaException extends RuntimeException {
    public CompraInvalidaException() {
        super("La compra contiene cantidades o precios inválidos");
    }
}
