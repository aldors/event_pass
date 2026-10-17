package com.aldo.event_pass.exception;

public class NoFuePosibleIniciarCheckoutException extends RuntimeException {
    public NoFuePosibleIniciarCheckoutException(Throwable cause) {
        super("No fue posible iniciar el pago con Stripe", cause);
    }
}
