package com.aldo.event_pass.mapper;

import org.springframework.stereotype.Component;

import com.aldo.event_pass.dto.auth.RegistroRequest;
import com.aldo.event_pass.dto.auth.RegistroResponse;
import com.aldo.event_pass.entity.Usuario;
import com.aldo.event_pass.enums.Role;

@Component
public class RegistroMapper {
    
    public static Usuario toEntity(RegistroRequest registroRequest){

        return Usuario.builder()
            .nombre(registroRequest.getNombre())
            .apellido(registroRequest.getApellido())
            .email(registroRequest.getEmail())
            .password(registroRequest.getPassword())
            .rol(Role.USER)
            .build();
    }

    public static RegistroResponse toResponse(Usuario usuario){

        return new RegistroResponse(
            usuario.getNombre(),
            usuario.getApellido(),
            usuario.getEmail(),
            usuario.getRol().name()
        );
    }

}
