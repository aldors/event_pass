package com.aldo.event_pass.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aldo.event_pass.entity.RefreshToken;
import com.aldo.event_pass.entity.Usuario;

@Repository 
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    Optional<RefreshToken> findByToken(String refreshToken);
    void deleteByToken(String refreshToken);
    void deleteByUsuario(Usuario usuario);
}
