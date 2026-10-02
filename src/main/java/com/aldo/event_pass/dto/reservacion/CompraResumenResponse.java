package com.aldo.event_pass.dto.reservacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.aldo.event_pass.enums.EstadoCompra;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CompraResumenResponse {
    
    private Long compraId;
    private String nombreEvento;
    private Long eventoId;
    private EstadoCompra estado;
    private int cantidadBoletos;
    private BigDecimal total;
    private LocalDateTime fechaReserva;
    private LocalDateTime fechaExpiracion;
}
