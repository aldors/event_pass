package com.aldo.event_pass.exception;

public class RefreshTokenNoEncontradoException extends RuntimeException {
    public RefreshTokenNoEncontradoException(){
        super("Refresh Token no encontrado");
    }
}