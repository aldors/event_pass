package com.aldo.event_pass.dto.evento;

import java.time.LocalDateTime;

import com.aldo.event_pass.enums.EstadoEvento;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Schema(description = "Evento en listado (vista resumida)")
public class EventoListadoResponse {
    
    @Schema(description = "ID del evento", example = "1")
    private Long id;
    @Schema(description = "Nombre del evento", example = "Concierto de Rock 2026")
    private String nombre;
    @Schema(description = "Ubicación del evento", example = "Auditorio Nacional, CDMX")
    private String ubicacion;
    @Schema(description = "Fecha y hora de inicio", example = "2026-11-20T19:00:00")
    private LocalDateTime fechaInicio;
    @Schema(description = "Estado del evento", example = "PUBLICADO")
    private EstadoEvento estado;
}
