package com.aldo.event_pass.exception;

public class ReservaExpiradaException extends RuntimeException {
    public ReservaExpiradaException(){
        super("La reserva expiró");
    }
}
