package com.aldo.event_pass.exception;

public class RefreshTokenNoValidoException extends RuntimeException {
    public RefreshTokenNoValidoException(){
        super("Refresh Token no válido");
    }
}
