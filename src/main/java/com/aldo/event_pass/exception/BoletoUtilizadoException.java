package com.aldo.event_pass.exception;

public class BoletoUtilizadoException extends RuntimeException {
    public BoletoUtilizadoException(){
        super("Este boleto ya fue utilizado");
    }
}
