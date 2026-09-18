package com.aldo.event_pass.exception;

public class FechaInicioFuturaException extends RuntimeException{
    public FechaInicioFuturaException(){
        super("La fecha de incicio del evento debe ser futura");
    }
}
