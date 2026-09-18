package com.aldo.event_pass.exception;

public class FechaFinPosteriorAInicioException extends RuntimeException{
    public FechaFinPosteriorAInicioException(){
        super("la fecha de finalización del evento debe ser posterior a la fecha de incio");
    }
}
