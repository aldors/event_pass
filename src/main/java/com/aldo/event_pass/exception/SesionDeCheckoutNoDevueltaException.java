package com.aldo.event_pass.exception;

public class SesionDeCheckoutNoDevueltaException extends RuntimeException {
    public SesionDeCheckoutNoDevueltaException() {
        super("Stripe no devolvió una URL de checkout");
    }
}
