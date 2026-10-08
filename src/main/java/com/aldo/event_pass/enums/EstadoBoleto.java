package com.aldo.event_pass.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado del boleto", example = "ACTIVO", allowableValues = {"ACTIVO", "UTILIZADO", "INVALIDADO"})
public enum EstadoBoleto {
    ACTIVO,
    UTILIZADO,
    INVALIDADO
}
