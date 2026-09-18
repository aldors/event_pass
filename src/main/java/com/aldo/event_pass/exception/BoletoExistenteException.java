package com.aldo.event_pass.exception;

public class BoletoExistenteException extends RuntimeException{
    public BoletoExistenteException(){
        super("Ya existe un boleto con este nombre");
    }
}
