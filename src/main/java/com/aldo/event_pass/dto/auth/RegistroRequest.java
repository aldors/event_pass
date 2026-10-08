package com.aldo.event_pass.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter 
@Getter 
@Schema(description = "Datos para registrar un usuario nuevo")
public class RegistroRequest {
    
    @Schema(description = "Nombre del usuario", example = "Aldo")
    @NotBlank(message = "El nombre es obligarorio")
    private String nombre;

    @Schema(description = "Apellido del usuario", example = "Ruiz")
    @NotBlank(message = "El apellido es obligarorio")
    private String apellido;

    @Schema(description = "Correo electrónico del usuario", example = "aldo@email.com", format = "email")
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico debe ser válido")
    private String email;

    @Schema(description = "Contraseña del usuario", example = "secreto123")
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;
}
