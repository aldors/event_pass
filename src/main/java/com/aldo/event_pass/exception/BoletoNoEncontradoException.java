package com.aldo.event_pass.exception;

public class BoletoNoEncontradoException extends RuntimeException {
    public BoletoNoEncontradoException(){
        super("Boleto no encontrado");
    }
}
