package com.aldo.event_pass.exception;

public class SesionAbiertaException extends WebhookNoAplicableException {
    public SesionAbiertaException() {
        super("La sesión de pago todavía está abierta");
    }
}
