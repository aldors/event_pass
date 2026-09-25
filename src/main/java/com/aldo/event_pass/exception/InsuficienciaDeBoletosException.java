package com.aldo.event_pass.exception;

public class InsuficienciaDeBoletosException extends RuntimeException {
    public InsuficienciaDeBoletosException(String tipoBoleto){
        super("No hay suficientes boletos disponibles para: " + tipoBoleto);
    }
}
