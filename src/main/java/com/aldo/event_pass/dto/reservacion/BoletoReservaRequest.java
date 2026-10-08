package com.aldo.event_pass.dto.reservacion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Un boleto dentro de la reserva")
public class BoletoReservaRequest {

    @Schema(description = "ID del tipo de boleto", example = "1")
    @NotNull(message = "El tipo de boleto es obligatorio")
    @Positive(message = "El id del tipo de boleto debe ser mayor a cero")
    private Long tipoBoletoId;

    @Schema(description = "Nombre del titular del boleto", example = "Aldo Rojas")
    @NotBlank(message = "El titular es obligatorio")
    private String titular;
}
