package com.aldo.event_pass.exception;

public class TipoBoletoNoPerteneceAlEventoException extends RuntimeException {
    public TipoBoletoNoPerteneceAlEventoException(){
        super("El tipo de boleto no pertenece a este evento");
    }
}
