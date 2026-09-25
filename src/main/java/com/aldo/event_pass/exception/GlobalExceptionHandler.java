package com.aldo.event_pass.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
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

    // Manejo de concurrencia si dos usuarios intentan registrar
    // el mismo email al mismo tiempo o cualquier violación de una restricción en la base de datos
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex) {

        ApiError apiError = new ApiError(
                HttpStatus.CONFLICT.value(),
                "Conflicto de integridad de datos",
                List.of("No fue posible procesar la solicitud debido a una restricción de datos")
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
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

    @ExceptionHandler(FechaFinPosteriorAInicioException.class)
    public ResponseEntity<ApiError> handleFechaFinPosteriorAInicio(FechaFinPosteriorAInicioException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Fechas invalidas",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    @ExceptionHandler(EventoNoEncontradoException.class)
    public ResponseEntity<ApiError> handleEventoNoEncontrado(EventoNoEncontradoException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.NOT_FOUND.value(),
                "Evento no enontrado",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }

    @ExceptionHandler(EventosEnBorradorException.class)
    public ResponseEntity<ApiError> handleEventosEnBorrador(EventosEnBorradorException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.CONFLICT.value(),
                "No se puede modificar el evento",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    @ExceptionHandler(BoletoExistenteException.class)
    public ResponseEntity<ApiError> handleBoletoExistente(BoletoExistenteException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.CONFLICT.value(),
                "Boleto existente",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    @ExceptionHandler(PublicarEventoException.class)
    public ResponseEntity<ApiError> handlePublicarEvento(PublicarEventoException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "No se puede publicar el evento",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    @ExceptionHandler(AlMenosUnBoletoException.class)
    public ResponseEntity<ApiError> handleAlMenosUnBoleto(AlMenosUnBoletoException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "El evento debe tener un boleto",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    @ExceptionHandler(FechaInicioFuturaException.class)
    public ResponseEntity<ApiError> handleFechaInicioFutura(FechaInicioFuturaException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "La fecha de inicio debe ser futura",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    @ExceptionHandler(EventoNoDisponibleException.class)
    public ResponseEntity<ApiError> handleEventoNoDisponible(EventoNoDisponibleException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.NOT_FOUND.value(),
                "Evento no disponible",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }

    @ExceptionHandler(ReservarBoletosEnEventosPublicadosException.class)
    public ResponseEntity<ApiError> handleReservarBoletosEnEventosPublicados(ReservarBoletosEnEventosPublicadosException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.CONFLICT.value(),
                "Mo puedes reservar boletos de este evento",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    @ExceptionHandler(EventoIniciadoException.class)
    public ResponseEntity<ApiError> handleEventoIniciado(EventoIniciadoException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.CONFLICT.value(),
                "El evento inició",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    @ExceptionHandler(ExcederMaximosBoletosPorUsuarioException.class)
    public ResponseEntity<ApiError> handleExcederMaximosBoletosPorUsuario(ExcederMaximosBoletosPorUsuarioException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.CONFLICT.value(),
                "No puedes exceder el límite de compra",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    @ExceptionHandler(TipoBoletoInexistenteException.class)
    public ResponseEntity<ApiError> handleTipoBoletoInexistente(TipoBoletoInexistenteException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.NOT_FOUND.value(),
                "Tipo de boleto inexistente",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }

    @ExceptionHandler(TipoBoletoNoEncontradoException.class)
    public ResponseEntity<ApiError> handleTipoBoletoNoEncontrado(TipoBoletoNoEncontradoException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.NOT_FOUND.value(),
                "Tipo de boleto no encontrado",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }

    @ExceptionHandler(TipoBoletoNoPerteneceAlEventoException.class)
    public ResponseEntity<ApiError> handleTipoBoletoNoPerteneceAlEvento(TipoBoletoNoPerteneceAlEventoException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.CONFLICT.value(),
                "El tipo de boleto no pertenece al evento",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    @ExceptionHandler(InsuficienciaDeBoletosException.class)
    public ResponseEntity<ApiError> handleInsuficienciaDeBoletos(InsuficienciaDeBoletosException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.CONFLICT.value(),
                "No hay suficientes boletos",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    @ExceptionHandler(CompraNoEncontradaException.class)
    public ResponseEntity<ApiError> handleCompraNoEncontrada(CompraNoEncontradaException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.NOT_FOUND.value(),
                "Compra no encontrada",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }

    @ExceptionHandler(CompraNoReservadaException.class)
    public ResponseEntity<ApiError> handleCompraNoEReservada(CompraNoReservadaException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.CONFLICT.value(),
                "Compra no reservada",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    @ExceptionHandler(ReservaExpiradaException.class)
    public ResponseEntity<ApiError> handleReservaExpirada(ReservaExpiradaException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.CONFLICT.value(),
                "Reserva expirada",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    @ExceptionHandler(BoletoNoEncontradoException.class)
    public ResponseEntity<ApiError> handleBoletoNoEncontrado(BoletoNoEncontradoException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.NOT_FOUND.value(),
                "Boleto no encontrado",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }

    @ExceptionHandler(BoletoUtilizadoException.class)
    public ResponseEntity<ApiError> handleBoletoUtilizado(BoletoUtilizadoException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.CONFLICT.value(),
                "Boleto utilizado",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    @ExceptionHandler(BoletoInvalidadoException.class)
    public ResponseEntity<ApiError> handleBoletoInvalidado(BoletoInvalidadoException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.CONFLICT.value(),
                "Boleto invalidado",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    @ExceptionHandler(EventoFinalizadoException.class)
    public ResponseEntity<ApiError> handleEventoFinalizado(EventoFinalizadoException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.CONFLICT.value(),
                "Evento finalizado",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    @ExceptionHandler(PermisoDenegadoParaGenerarBoletoException.class)
    public ResponseEntity<ApiError> handlePermisoDenegadoParaGenerarBoleto(PermisoDenegadoParaGenerarBoletoException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.FORBIDDEN.value(),
                "No puedes descargar este boleto",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiError);
    }

    @ExceptionHandler(GeneracionPdfException.class)
    public ResponseEntity<ApiError> handleGeneracionPdf(GeneracionPdfException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "No fue posible generar el boleto pdf",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
    }

    @ExceptionHandler(GeneracionQrException.class)
    public ResponseEntity<ApiError> handleGeneracionQr(GeneracionQrException ex) {
            
        ApiError apiError = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "No fue posible generar el código Qr",
                List.of(ex.getMessage())
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
    }

}
