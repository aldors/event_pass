package com.aldo.event_pass.dto.boleto;

import com.aldo.event_pass.enums.EstadoBoleto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Resultado de marcar un boleto como usado en acceso")
public class UsarBoletoResponse {

    @Schema(description = "Nombre del evento", example = "Concierto de Rock 2026")
    private String evento;
    @Schema(description = "Titular del boleto", example = "Aldo Rojas")
    private String titular;
    @Schema(description = "Folio / código QR", example = "EVT-AB12CD34")
    private String folio;
    @Schema(description = "Estado final del boleto", example = "UTILIZADO")
    private EstadoBoleto estado;
    @Schema(description = "Mensaje de confirmación", example = "Ingreso registrado correctamente")
    private String mensaje;
}
