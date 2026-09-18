package com.aldo.event_pass.exception;

public class PublicarEventoException extends RuntimeException{
    public PublicarEventoException(){
        super("Solo eventos en borrador pueden publicarse");
    }
}
