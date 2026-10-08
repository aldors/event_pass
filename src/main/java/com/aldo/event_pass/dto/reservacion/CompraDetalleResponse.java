package com.aldo.event_pass.dto.reservacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.aldo.event_pass.dto.boleto.BoletoDetalleResponse;
import com.aldo.event_pass.enums.EstadoCompra;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Detalle de una compra con sus boletos")
public class CompraDetalleResponse {

    @Schema(description = "ID de la compra", example = "1")
    private Long compraId;
    @Schema(description = "Nombre del evento", example = "Concierto de Rock 2026")
    private String nombreEvento;
    @Schema(description = "ID del evento", example = "1")
    private Long eventoId;
    @Schema(description = "Estado de la compra", example = "PAGADA")
    private EstadoCompra estado;
    @Schema(description = "Cantidad de boletos", example = "2")
    private int cantidadBoletos;
    @Schema(description = "Total de la compra", example = "500.00")
    private BigDecimal total;
    @Schema(description = "Fecha de reserva", example = "2026-10-08T10:00:00")
    private LocalDateTime fechaReserva;
    @Schema(description = "Fecha de expiración (si sigue pendiente)", example = "2026-10-08T10:15:00")
    private LocalDateTime fechaExpiracion;
    @Schema(description = "Boletos de la compra")
    private List<BoletoDetalleResponse> boletos;
}
