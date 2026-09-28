package com.aldo.event_pass.exception;

public class CancelarEventoException extends RuntimeException {
    public CancelarEventoException(){
        super("Solo eventos publicados pueden cancelarse");
    }
}
