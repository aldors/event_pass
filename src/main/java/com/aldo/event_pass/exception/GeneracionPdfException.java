package com.aldo.event_pass.exception;

public class GeneracionPdfException extends RuntimeException {
    public GeneracionPdfException(Throwable cause) {
        super("Error al generar el PDF del boleto", cause);
    }
}