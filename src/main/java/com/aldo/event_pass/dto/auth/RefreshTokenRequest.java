package com.aldo.event_pass.dto.auth;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class RefreshTokenRequest {
    
    private String refreshToken;
}
