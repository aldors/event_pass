package com.aldo.event_pass.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado del pago", example = "APROBADO", allowableValues = {"PENDIENTE", "APROBADO", "RECHAZADO"})
public enum EstadoPago {
    PENDIENTE,
    APROBADO,
    RECHAZADO
}
