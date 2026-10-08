package com.aldo.event_pass.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter 
@Schema(description = "Datos del usuario recién registrado")
public class RegistroResponse {

    @Schema(description = "Nombre del usuario", example = "Aldo")
    private String nombre;
    @Schema(description = "Apellido del usuario", example = "Rojas")
    private String apellido;
    @Schema(description = "Correo del usuario", example = "aldo@email.com")
    private String email;
    @Schema(description = "Rol asignado", example = "USER")
    private String rol;
}
