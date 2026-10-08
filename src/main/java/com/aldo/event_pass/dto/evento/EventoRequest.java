package com.aldo.event_pass.dto.evento;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Datos para crear un evento")
public class EventoRequest {
    
    @Schema(description = "Nombre del evento", example = "Concierto de Rock 2026")
    @NotBlank(message = "El nombre del evento es obligatorio")
    private String nombre;

    @Schema(description = "Descripción del evento", example = "Concierto en vivo con bandas invitadas")
    @NotBlank(message = "La descripción del evento es obligatoria")
    private String descripcion;

    @Schema(description = "Fecha y hora de inicio", example = "2026-11-20T19:00:00")
    @NotNull(message = "La fecha de inicio es obligatoria")
    @FutureOrPresent(message = "La fecha de inicio debe ser actual o futura")
    private LocalDateTime fechaInicio;

    @Schema(description = "Fecha y hora de fin", example = "2026-11-20T23:00:00")
    @NotNull(message = "La fecha de fin es obligatoria")
    @Future(message = "La fecha de fin debe ser futura")
    private LocalDateTime fechaFin;

    @Schema(description = "Ubicación del evento", example = "Auditorio Nacional, CDMX")
    @NotBlank(message = "La ubicación del evento es obligatoria")
    private String ubicacion;

    @Schema(description = "Límite de boletos por usuario", example = "4")
    @NotNull(message = "El límite de boletos por usuario es obligatorio")
    @Min(value = 1, message = "Debe ser mayor a cero")
    private Integer maxBoletosPorUsuario;
}
