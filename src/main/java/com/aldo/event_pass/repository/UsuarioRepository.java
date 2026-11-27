package com.aldo.event_pass.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.aldo.event_pass.entity.Usuario;

import jakarta.persistence.LockModeType;

@Repository 
public interface UsuarioRepository extends JpaRepository<Usuario, Long>{
    
    Optional<Usuario> findByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT u
        FROM Usuario u
        WHERE u.id = :id
    """)
    Optional<Usuario> findByIdForUpdate(@Param("id") Long id);
}
