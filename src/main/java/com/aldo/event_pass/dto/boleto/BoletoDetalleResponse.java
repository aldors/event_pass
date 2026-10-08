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
    @Schema(description = "Titular del boleto", example = "Aldo Rojas")
    private String titularNombre;
    @Schema(description = "Folio / código QR", example = "EVT-AB12CD34")
    private String folio;
    @Schema(description = "Estado del boleto", example = "ACTIVO")
    private EstadoBoleto estado;
    /*
     * Indica si el PDF puede descargarse (compra pagada y evento vigente).
     * El frontend solo lee este flag, no replica las reglas.
     */
    @Schema(description = "Indica si el PDF puede descargarse", example = "true")
    private boolean descargable;
}
