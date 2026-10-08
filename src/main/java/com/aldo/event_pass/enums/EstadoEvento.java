package com.aldo.event_pass.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado del evento", example = "PUBLICADO", allowableValues = {"BORRADOR", "PUBLICADO", "FINALIZADO", "CANCELADO"})
public enum EstadoEvento {
    BORRADOR,
    PUBLICADO,
    FINALIZADO,
    CANCELADO
}
