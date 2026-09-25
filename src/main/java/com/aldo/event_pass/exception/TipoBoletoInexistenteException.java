package com.aldo.event_pass.exception;

public class TipoBoletoInexistenteException extends RuntimeException {
    public TipoBoletoInexistenteException(){
        super("Uno o más tipos de boleto no existen");
    }
}
