package com.aldo.event_pass.exception;

public class CompraNoEncontradaException extends RuntimeException {
    public CompraNoEncontradaException(){
        super("Compra no encontrada");
    }
}
