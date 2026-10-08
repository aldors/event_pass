package com.aldo.event_pass.dto.reservacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Reserva creada, pendiente de pago")
public class ReservaResponse {

    @Schema(description = "ID de la compra generada", example = "1")
    private Long compraId;
    @Schema(description = "Total a pagar", example = "500.00")
    private BigDecimal total;
    @Schema(description = "Fecha límite para pagar", example = "2026-10-09T12:00:00")
    private LocalDateTime expiraEn;
}
