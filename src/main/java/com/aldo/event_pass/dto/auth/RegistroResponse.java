package com.aldo.event_pass.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter 
public class RegistroResponse {

    private String nombre;
    private String apellido;
    private String email;
    private String rol;
}
