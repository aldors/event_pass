package com.aldo.event_pass.exception;

public class TipoBoletoNoEncontradoException extends RuntimeException {
    public TipoBoletoNoEncontradoException(){
        super("Tipo de boleto no encontrado");
    }
}
