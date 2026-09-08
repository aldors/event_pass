package com.aldo.event_pass.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class LoginResponse {
    
    private String accessToken;
    private String refreshToken;
}
