package com.aldo.event_pass.dto.evento;

import java.time.LocalDateTime;
import java.util.List;

import com.aldo.event_pass.dto.tipo_boleto.TipoBoletoDisponibleResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class EventoDetalleResponse {
    
    private Long id;
    private String nombre;
    private String descripcion;
    private String ubicacion;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Integer maxBoletosPorUsuario;
    private List<TipoBoletoDisponibleResponse> tiposBoleto;
}
