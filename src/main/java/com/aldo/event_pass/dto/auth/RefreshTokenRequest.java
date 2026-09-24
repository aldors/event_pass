package com.aldo.event_pass.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class RefreshTokenRequest {
    
    @NotBlank(message = "El refresh token es obligatorio")
    private String refreshToken;
}
