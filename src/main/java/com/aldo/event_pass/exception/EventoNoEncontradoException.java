package com.aldo.event_pass.exception;

public class EventoNoEncontradoException extends RuntimeException{
    public EventoNoEncontradoException(){
        super("Evento no encontrado");
    }
}
