package com.aldo.event_pass.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.aldo.event_pass.exception.ApiError.ApiError;

@RestControllerAdvice 
public class GlobalExceptionHandler {
    
    // Manejo de erorres @valid en DTOs de tipo request
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.toList());

        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Error de validación",
                errors
        );

        return ResponseEntity.badRequest().body(apiError);
    }

    // Manejo de errores de autenticación, como credenciales incorrectas al iniciar sesión
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {

        ApiError apiError = new ApiError(
                HttpStatus.UNAUTHORIZED.value(),
                "Credenciales incorrectas",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiError);
    }

    // Manejo general de excepciones no controladas
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Error interno del servidor",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
    }

    /*
        EXCEPCIONES PERSONALIZADAS
    */

    @ExceptionHandler(EmailExistenteException.class)
    public ResponseEntity<ApiError> handleEmailExistente(EmailExistenteException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.CONFLICT.value(),
                "Email en uso por otro usuario",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    @ExceptionHandler(RefreshTokenNoEncontradoException.class)
    public ResponseEntity<ApiError> handleRefreshTokenNoEncontrado(RefreshTokenNoEncontradoException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.NOT_FOUND.value(),
                "Refresh Token no encontrado",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }

    @ExceptionHandler(RefreshTokenExpiradoException.class)
    public ResponseEntity<ApiError> handleRefreshTokenExpirado(RefreshTokenExpiradoException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.UNAUTHORIZED.value(),
                "Refresh Token expirado",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiError);
    }

    @ExceptionHandler(RefreshTokenNoValidoException.class)
    public ResponseEntity<ApiError> handleRefreshTokenNoValido(RefreshTokenNoValidoException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.UNAUTHORIZED.value(),
                "Refresh Token inválido",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiError);
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<ApiError> handleUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.NOT_FOUND.value(),
                "Usuario no encontrado",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }

}
