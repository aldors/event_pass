package com.aldo.event_pass.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Datos para cerrar la sesión")
public class LogoutRequest {
    
    @Schema(description = "Refresh token a invalidar", example = "eyJhbGciOiJIUzI1NiJ9...")
    @NotBlank(message = "El refresh token es obligatorio")
    private String refreshToken;
}
