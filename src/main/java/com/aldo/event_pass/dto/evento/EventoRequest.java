package com.aldo.event_pass.dto.evento;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventoRequest {
    
    @NotBlank(message = "El nombre del evento es obligatorio")
    private String nombre;

    @NotBlank(message = "La descripción del evento es obligatoria")
    private String descripcion;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @FutureOrPresent(message = "La fecha de inicio debe ser actual o futura")
    private LocalDateTime fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    @Future(message = "La fecha de fin debe ser futura")
    private LocalDateTime fechaFin;

    @NotBlank(message = "La ubicación del evento es obligatoria")
    private String ubicacion;

    @NotNull(message = "El límite de boletos por usuario es obligatorio")
    @Min(value = 1, message = "Debe ser mayor a cero")
    private Integer maxBoletosPorUsuario;
}
