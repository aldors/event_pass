package com.aldo.event_pass.dto.reservacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoletoReservaRequest {

    @NotNull(message = "El tipo de boleto es obligatorio")
    private Long tipoBoletoId;

    @NotBlank(message = "El titular es obligatorio")
    private String titular;
}
