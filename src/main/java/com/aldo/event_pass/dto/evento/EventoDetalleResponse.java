package com.aldo.event_pass.dto.evento;

import java.time.LocalDateTime;
import java.util.List;

import com.aldo.event_pass.dto.tipo_boleto.TipoBoletoDisponibleResponse;
import com.aldo.event_pass.enums.EstadoEvento;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Schema(description = "Detalle de un evento con sus tipos de boleto")
public class EventoDetalleResponse {
    
    @Schema(description = "ID del evento", example = "1")
    private Long id;
    @Schema(description = "Nombre del evento", example = "Concierto de Rock 2026")
    private String nombre;
    @Schema(description = "Descripción del evento", example = "Concierto en vivo con bandas invitadas")
    private String descripcion;
    @Schema(description = "Ubicación del evento", example = "Auditorio Nacional, CDMX")
    private String ubicacion;
    @Schema(description = "Fecha y hora de inicio", example = "2026-11-20T19:00:00")
    private LocalDateTime fechaInicio;
    @Schema(description = "Fecha y hora de fin", example = "2026-11-20T23:00:00")
    private LocalDateTime fechaFin;
    @Schema(description = "Límite de boletos por usuario", example = "4")
    private Integer maxBoletosPorUsuario;
    @Schema(description = "Estado del evento", example = "PUBLICADO")
    private EstadoEvento estado;
    @Schema(description = "Tipos de boleto disponibles")
    private List<TipoBoletoDisponibleResponse> tiposBoleto;
}
