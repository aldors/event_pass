package com.aldo.event_pass.exception;

public class EventoCanceladoException extends RuntimeException {
    public EventoCanceladoException(){
        super("El evento ha sido cancelado");
    }
}
