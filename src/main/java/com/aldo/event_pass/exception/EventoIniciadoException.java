package com.aldo.event_pass.exception;

public class EventoIniciadoException extends RuntimeException{
    public EventoIniciadoException(){
        super("No puedes reservar boletos de un evento que ya inició");
    }
}
