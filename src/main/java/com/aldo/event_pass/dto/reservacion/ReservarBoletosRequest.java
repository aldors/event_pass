package com.aldo.event_pass.dto.reservacion;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReservarBoletosRequest {

    @NotNull(message = "El evento es obligatorio")
    private Long eventoId;

    @NotEmpty(message = "Debe seleccionar al menos un boleto")
    private List<BoletoReservaRequest> boletos;
}
