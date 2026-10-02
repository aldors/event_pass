package com.aldo.event_pass.dto.reservacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.aldo.event_pass.dto.boleto.BoletoDetalleResponse;
import com.aldo.event_pass.enums.EstadoCompra;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CompraDetalleResponse {

    private Long compraId;
    private String nombreEvento;
    private Long eventoId;
    private EstadoCompra estado;
    private int cantidadBoletos;
    private BigDecimal total;
    private LocalDateTime fechaReserva;
    private LocalDateTime fechaExpiracion;
    private List<BoletoDetalleResponse> boletos;
}
