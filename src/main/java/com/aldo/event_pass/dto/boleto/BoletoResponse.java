package com.aldo.event_pass.dto.boleto;

import com.aldo.event_pass.enums.EstadoBoleto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Estado de un boleto al verificar su QR")
public class BoletoResponse {

    @Schema(description = "Nombre del evento", example = "Concierto de Rock 2026")
    private String evento;
    @Schema(description = "Tipo de boleto", example = "General")
    private String tipoBoleto;
    @Schema(description = "Titular del boleto", example = "Aldo Rojas")
    private String titular;
    @Schema(description = "Folio / código QR", example = "EVT-AB12CD34")
    private String folio;
    @Schema(description = "Estado del boleto", example = "ACTIVO")
    private EstadoBoleto estado;
    @Schema(description = "Indica si el boleto es válido para ingreso", example = "true")
    private boolean valido;
}
