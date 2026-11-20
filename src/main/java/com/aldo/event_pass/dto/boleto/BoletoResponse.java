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
    @Schema(description = "Folio del boleto (UUID distinto del código QR escaneado)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String folio;
    @Schema(description = "Estado del boleto", example = "ACTIVO")
    private EstadoBoleto estado;
    @Schema(description = "Válido para ingreso: boleto ACTIVO, compra PAGADA y evento efectivo PUBLICADO o INICIADO", example = "true")
    private boolean valido;
}
