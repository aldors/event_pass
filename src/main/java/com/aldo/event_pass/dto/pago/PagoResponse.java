package com.aldo.event_pass.dto.pago;

import java.math.BigDecimal;

import com.aldo.event_pass.enums.EstadoPago;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PagoResponse {

    private Long compraId;
    private EstadoPago estado;
    private BigDecimal monto;
}
