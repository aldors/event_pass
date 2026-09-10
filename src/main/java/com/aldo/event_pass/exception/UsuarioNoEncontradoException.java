package com.aldo.event_pass.exception;

public class UsuarioNoEncontradoException extends RuntimeException {
    public UsuarioNoEncontradoException(){
        super("El usuario no ha sido encontrado");
    }
}
