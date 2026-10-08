package com.aldo.event_pass.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Rol del usuario", example = "USER", allowableValues = {"USER", "ADMIN"})
public enum Role {
    USER,
    ADMIN
}
