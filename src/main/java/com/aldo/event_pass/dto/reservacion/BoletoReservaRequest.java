package com.aldo.event_pass.dto.reservacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoletoReservaRequest {

    @NotNull(message = "El tipo de boleto es obligatorio")
    @Positive(message = "El id del tipo de boleto debe ser mayor a cero")
    private Long tipoBoletoId;

    @NotBlank(message = "El titular es obligatorio")
    private String titular;
}
