package com.aldo.event_pass.dto.boleto;

import com.aldo.event_pass.enums.EstadoBoleto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Boleto dentro del detalle de compra")
public class BoletoDetalleResponse {

    @Schema(description = "ID del boleto", example = "10")
    private Long boletoId;
    @Schema(description = "Nombre del tipo de boleto", example = "General")
    private String tipoBoletoNombre;
    @Schema(description = "Titular del boleto", example = "Aldo Ruiz")
    private String titularNombre;
    @Schema(description = "Folio del boleto (UUID distinto del código QR escaneado)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String folio;
    @Schema(description = "Estado del boleto", example = "ACTIVO")
    private EstadoBoleto estado;
    @Schema(description = "PDF descargable si la compra está PAGADA y el estado efectivo del evento no es CANCELADO ni FINALIZADO (INICIADO permitido); el endpoint exige ser el dueño", example = "true")
    private boolean descargable;
}
