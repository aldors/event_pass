package com.aldo.event_pass.exception;

public class ReservarBoletosEnEventosPublicadosException extends RuntimeException {
    public ReservarBoletosEnEventosPublicadosException(){
        super("Solo puedes reservar boletos de eventos publicados");
    }
}
