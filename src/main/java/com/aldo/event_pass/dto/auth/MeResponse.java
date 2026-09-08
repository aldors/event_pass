package com.aldo.event_pass.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor 
@Getter 
public class MeResponse {
    
    private String nombre;
    private String email;
}
