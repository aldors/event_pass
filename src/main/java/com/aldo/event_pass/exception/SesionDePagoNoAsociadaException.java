package com.aldo.event_pass.exception;

public class SesionDePagoNoAsociadaException extends WebhookNoAplicableException {
    public SesionDePagoNoAsociadaException() {
        super("No existe un pago asociado a la sesión de Stripe");
    }
}
