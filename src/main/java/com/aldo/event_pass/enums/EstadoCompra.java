package com.aldo.event_pass.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado de la compra", example = "PAGADA", allowableValues = {"RESERVADA", "PAGADA", "CANCELADA", "EXPIRADA"})
public enum EstadoCompra {
    RESERVADA,
    PAGADA,
    CANCELADA,
    EXPIRADA
}
