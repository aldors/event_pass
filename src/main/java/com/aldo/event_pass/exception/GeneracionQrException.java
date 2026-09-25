package com.aldo.event_pass.exception;

public class GeneracionQrException extends RuntimeException {
    public GeneracionQrException(Throwable cause) {
        super("Error al generar el código QR", cause);
    }
}