package com.aldo.event_pass.exception;

public class PermisoDenegadoParaGenerarBoletoException extends RuntimeException {
    public PermisoDenegadoParaGenerarBoletoException(){
        super("No tienes permiso para descargar este boleto");
    }
}
