package com.aldo.event_pass.exception;

public class CompraNoValidaParaPagarException extends WebhookNoAplicableException {
    public CompraNoValidaParaPagarException() {
        super("La compra ya no está en un estado válido para pagar");
    }
}
