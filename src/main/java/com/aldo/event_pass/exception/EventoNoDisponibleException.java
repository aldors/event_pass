package com.aldo.event_pass.exception;

public class EventoNoDisponibleException extends RuntimeException{
    public EventoNoDisponibleException(){
        super("Este evento no está disponible");
    }
}
