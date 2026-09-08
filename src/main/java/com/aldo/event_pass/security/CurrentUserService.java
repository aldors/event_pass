package com.aldo.event_pass.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.aldo.event_pass.entity.Usuario;
import com.aldo.event_pass.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class CurrentUserService {

    private final UsuarioRepository usuarioRepository;
    
    public Usuario obtenerUsuarioActual(){
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        return usuarioRepository.findByEmail(email).orElseThrow(() -> new RuntimeException());
    }
}
