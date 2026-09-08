package com.aldo.event_pass.service.interfaces;

import com.aldo.event_pass.dto.auth.LoginRequest;
import com.aldo.event_pass.dto.auth.LoginResponse;
import com.aldo.event_pass.dto.auth.LogoutRequest;
import com.aldo.event_pass.dto.auth.MeResponse;
import com.aldo.event_pass.dto.auth.RefreshTokenRequest;
import com.aldo.event_pass.dto.auth.RegistroRequest;
import com.aldo.event_pass.dto.auth.RegistroResponse;

public interface AuthService {
    
    public RegistroResponse registro(RegistroRequest registroRequest);
    public LoginResponse login(LoginRequest loginRequest);
    public LoginResponse refreshToken(RefreshTokenRequest refreshTokenRequest);
    public String logout(LogoutRequest logoutRequest);
    public MeResponse me();
}
