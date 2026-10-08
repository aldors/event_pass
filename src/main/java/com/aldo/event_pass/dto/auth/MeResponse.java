package com.aldo.event_pass.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor 
@Getter 
@Schema(description = "Perfil del usuario autenticado")
public class MeResponse {
    
    @Schema(description = "Nombre del usuario", example = "Aldo")
    private String nombre;
    @Schema(description = "Correo del usuario", example = "aldo@email.com")
    private String email;
    @Schema(description = "Rol del usuario", example = "USER")
    private String rol;
}
