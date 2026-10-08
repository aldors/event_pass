package com.aldo.event_pass.dto.pago;

import java.math.BigDecimal;

import com.aldo.event_pass.enums.EstadoPago;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Schema(description = "Resultado del pago de una compra")
public class PagoResponse {

    @Schema(description = "ID de la compra pagada", example = "1")
    private Long compraId;
    @Schema(description = "Estado del pago", example = "APROBADO")
    private EstadoPago estado;
    @Schema(description = "Monto pagado", example = "500.00")
    private BigDecimal monto;
}
