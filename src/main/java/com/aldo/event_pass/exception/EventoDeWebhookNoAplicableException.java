package com.aldo.event_pass.exception;

public class EventoDeWebhookNoAplicableException extends WebhookNoAplicableException {
    public EventoDeWebhookNoAplicableException() {
        super("No se pudo interpretar el evento de Stripe");
    }
}
