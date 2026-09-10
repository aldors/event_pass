package com.aldo.event_pass.exception;

public class EmailExistenteException extends RuntimeException {
    public EmailExistenteException(String email){
        super("El email '" + email + "' ya está en uso");
    }
}
