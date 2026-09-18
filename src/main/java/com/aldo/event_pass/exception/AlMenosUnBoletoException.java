package com.aldo.event_pass.exception;

public class AlMenosUnBoletoException extends RuntimeException{
    public AlMenosUnBoletoException(){
        super("El evento debe tener al menos un boleto");
    }
}
