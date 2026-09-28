package com.aldo.event_pass.exception;

public class EliminarBorradorException extends RuntimeException {
    public EliminarBorradorException(){
        super("Solo eventos en borrador pueden eliminarse");
    }
}
