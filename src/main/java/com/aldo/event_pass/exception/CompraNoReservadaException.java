package com.aldo.event_pass.exception;

public class CompraNoReservadaException extends RuntimeException {
    public CompraNoReservadaException(){
        super("La compra no está reservada");
    }
}
