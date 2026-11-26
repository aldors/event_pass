package com.aldo.event_pass.dto.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.aldo.event_pass.enums.EstadoCompra;

public interface CompraResumenProjection {

    Long getCompraId();

    String getNombreEvento();

    Long getEventoId();

    EstadoCompra getEstado();

    Long getCantidadBoletos();

    BigDecimal getTotal();

    LocalDateTime getFechaReserva();

    LocalDateTime getFechaExpiracion();
}
