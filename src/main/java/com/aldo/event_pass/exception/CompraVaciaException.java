package com.aldo.event_pass.exception;

public class CompraVaciaException extends RuntimeException {
    public CompraVaciaException() {
        super("La compra no contiene detalles");
    }
}
