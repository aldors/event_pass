package com.aldo.event_pass.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.aldo.event_pass.entity.Compra;

import jakarta.persistence.LockModeType;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Compra> findByIdAndUsuarioId(Long compraId, Long usuarioId);
}
