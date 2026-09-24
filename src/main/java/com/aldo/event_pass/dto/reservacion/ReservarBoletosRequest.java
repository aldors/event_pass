package com.aldo.event_pass.dto.reservacion;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReservarBoletosRequest {

    @NotNull(message = "El evento es obligatorio")
    @Positive(message = "El id del evento debe ser mayor a cero")
    private Long eventoId;

    @Valid
    @NotEmpty(message = "Debe seleccionar al menos un boleto")
    private List<BoletoReservaRequest> boletos;
}
