package com.aldo.event_pass.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aldo.event_pass.entity.Compra;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    
    Optional<Compra> findByIdAndUsuarioId(Long compraId, Long usuarioId);
}
