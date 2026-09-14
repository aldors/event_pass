package com.aldo.event_pass.dto.evento;

import java.time.LocalDateTime;

import com.aldo.event_pass.enums.EstadoEvento;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class EventoListadoResponse {
    
    private Long id;
    private String nombre;
    private String ubicacion;
    private LocalDateTime fechaInicio;
    private EstadoEvento estado;
}
