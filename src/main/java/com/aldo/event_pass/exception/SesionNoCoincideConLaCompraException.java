package com.aldo.event_pass.exception;

public class SesionNoCoincideConLaCompraException extends WebhookNoAplicableException {
    public SesionNoCoincideConLaCompraException() {
        super("La sesión de pago no coincide con la compra");
    }
}
