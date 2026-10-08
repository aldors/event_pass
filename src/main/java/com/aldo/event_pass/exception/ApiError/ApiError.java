package com.aldo.event_pass.exception.ApiError;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Formato estándar de error de la API")
public class ApiError {
    
    @Schema(description = "Código HTTP", example = "400")
    private int status;
    @Schema(description = "Título del error", example = "Error de validación")
    private String message;
    @Schema(description = "Fecha y hora del error", example = "2026-10-08T10:00:00")
    private LocalDateTime timestamp;
    @Schema(description = "Detalle de errores", example = "[\"Detalle del error\"]")
    private List<String> errors;

    public ApiError(int status, String message, List<String> errors) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.errors = errors;
    }
}
