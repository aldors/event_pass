package com.aldo.event_pass.dto.pago;

import java.math.BigDecimal;

import com.aldo.event_pass.enums.EstadoCompra;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PagoResponse {

    private Long compraRId;
    private EstadoCompra estado;
    private BigDecimal total;
}
