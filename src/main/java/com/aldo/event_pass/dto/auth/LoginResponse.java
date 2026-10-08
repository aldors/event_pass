package com.aldo.event_pass.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Schema(description = "Tokens generados al iniciar sesión o renovarlos")
public class LoginResponse {
    
    @Schema(description = "JWT de acceso", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;
    @Schema(description = "Token para renovar la sesión", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;
}
