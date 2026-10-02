package com.aldo.event_pass.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aldo.event_pass.entity.Compra;

import jakarta.persistence.LockModeType;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Compra> findByIdAndUsuarioId(Long compraId, Long usuarioId);

    @Modifying
    @Query("""
    UPDATE Compra c
    SET c.estado = 'EXPIRADA'
    WHERE c.estado = 'RESERVADA'
    AND c.fechaExpiracionReserva < CURRENT_TIMESTAMP
    """)
    int expirarReservasVencidas();

    @Modifying
    @Query("""
    UPDATE Compra c
    SET c.estado = 'CANCELADA'
    WHERE c.estado = 'RESERVADA'
    AND EXISTS (
        SELECT 1 FROM DetalleCompra dc
        WHERE dc.compra = c
        AND dc.tipoBoleto.evento.id = :eventoId
    )
    """)
    int cancelarReservasPorEvento(@Param("eventoId") Long eventoId);

    @Modifying
    @Query("""
    UPDATE Compra c
    SET c.estado = 'EXPIRADA'
    WHERE c.estado = 'RESERVADA'
    AND EXISTS (
        SELECT 1 FROM DetalleCompra dc
        WHERE dc.compra = c
        AND dc.tipoBoleto.evento.id = :eventoId
    )
    """)
    int expirarReservasPorEvento(@Param("eventoId") Long eventoId);

    List<Compra> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    @Query("""
    SELECT c FROM Compra c
    WHERE c.id = :compraId
    AND c.usuario.id = :usuarioId
    """)
    Optional<Compra> buscarMiCompra(@Param("compraId") Long compraId, @Param("usuarioId") Long usuarioId);
}
