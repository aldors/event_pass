package com.aldo.event_pass.exception;

/*
 * Fallo transitorio al consultar la API de Stripe (red, timeout, 5xx).
 * A diferencia de los errores de negocio del webhook, este SÍ admite
 * reintento: el webhook debe responder 500 y verificar-pago 500/502.
 */
public class StripeNoDisponibleException extends RuntimeException {
    public StripeNoDisponibleException(String message, Throwable cause){
        super(message, cause);
    }
}
