package com.aldo.event_pass.exception;

public class CompraYaPagadaException extends RuntimeException {
    public CompraYaPagadaException(){
        super("La compra ya fue pagada");
    }
}
