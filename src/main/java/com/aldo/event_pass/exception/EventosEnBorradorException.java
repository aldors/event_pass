package com.aldo.event_pass.exception;

public class EventosEnBorradorException extends RuntimeException{
    public EventosEnBorradorException(){
        super("Permitido únicamente agregar boletos a eventos en borrador");
    }
}
