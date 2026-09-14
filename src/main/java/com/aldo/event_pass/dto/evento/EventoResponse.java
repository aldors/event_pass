package com.aldo.event_pass.dto.evento;

import java.time.LocalDateTime;

import com.aldo.event_pass.enums.EstadoEvento;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventoResponse {
    
    private Long id;
    private String nombre;
    private String descripcion;
    private String ubicacion;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Integer maxBoletosPorUsuario;
    private EstadoEvento estado;
}
