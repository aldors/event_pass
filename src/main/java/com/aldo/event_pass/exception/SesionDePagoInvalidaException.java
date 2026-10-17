package com.aldo.event_pass.exception;

public class SesionDePagoInvalidaException extends WebhookNoAplicableException {
    public SesionDePagoInvalidaException() {
        super("Stripe no devolvió un estado válido para la sesión de pago");
    }

    public SesionDePagoInvalidaException(String estado) {
        super("Estado de Checkout no reconocido: " + estado);
    }
}
