package com.aldo.event_pass.exception;

public class ExcederMaximosBoletosPorUsuarioException extends RuntimeException {
    public ExcederMaximosBoletosPorUsuarioException(){
        super("No puedes exceder el limite de compra de boletos por usuario");
    }
}
