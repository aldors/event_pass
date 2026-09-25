package com.aldo.event_pass.exception;

public class BoletoInvalidadoException extends RuntimeException {
    public BoletoInvalidadoException(){
        super("Este boleto fue invalidado");
    }
}
