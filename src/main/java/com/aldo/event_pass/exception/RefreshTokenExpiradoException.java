package com.aldo.event_pass.exception;

public class RefreshTokenExpiradoException extends RuntimeException {
    public RefreshTokenExpiradoException(){
        super("El Refresh Token ha expirado");
    }
}
