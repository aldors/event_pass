package com.aldo.event_pass.exception;

public class BoletoNoPagadoException extends RuntimeException {
    public BoletoNoPagadoException(){
        super("El boleto no está pagado");
    }
}
