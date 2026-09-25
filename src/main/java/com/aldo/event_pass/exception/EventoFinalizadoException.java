package com.aldo.event_pass.exception;

public class EventoFinalizadoException extends RuntimeException {
    public EventoFinalizadoException(){
        super("El evento ya finalizó");
    }
}
