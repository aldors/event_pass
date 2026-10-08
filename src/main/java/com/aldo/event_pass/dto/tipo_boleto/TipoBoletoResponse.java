package com.aldo.event_pass.dto.tipo_boleto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Tipo de boleto creado")
public class TipoBoletoResponse {
    
    @Schema(description = "ID del tipo de boleto", example = "1")
    private Long id;
    @Schema(description = "Nombre del tipo de boleto", example = "General")
    private String nombre;
    @Schema(description = "Precio del boleto", example = "250.00")
    private BigDecimal precio;
    @Schema(description = "Cantidad total", example = "100")
    private Integer cantidadTotal;
}
