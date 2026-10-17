package com.aldo.event_pass.exception;

public class ImporteDePagoNoCoincidenteException extends WebhookNoAplicableException {
    public ImporteDePagoNoCoincidenteException() {
        super("El importe o la moneda del pago no coinciden");
    }
}
