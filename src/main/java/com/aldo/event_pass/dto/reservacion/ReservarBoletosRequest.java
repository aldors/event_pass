package com.aldo.event_pass.dto.reservacion;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Datos para reservar boletos de un evento")
public class ReservarBoletosRequest {

    @Schema(description = "ID del evento", example = "1")
    @NotNull(message = "El evento es obligatorio")
    @Positive(message = "El id del evento debe ser mayor a cero")
    private Long eventoId;

    @Schema(description = "Boletos a reservar (uno por titular)")
    @Valid
    @NotEmpty(message = "Debe seleccionar al menos un boleto")
    private List<BoletoReservaRequest> boletos;
}
