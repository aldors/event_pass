package com.aldo.event_pass.mapper;

import org.springframework.stereotype.Component;

import com.aldo.event_pass.dto.auth.LoginResponse;

@Component 
public class LoginMapper {
    
    public static LoginResponse toResponse(String accessToken, String refreshToken){
        return new LoginResponse(accessToken, refreshToken);
    }
}
