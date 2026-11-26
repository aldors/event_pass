package com.aldo.event_pass.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aldo.event_pass.dto.projection.CompraResumenProjection;
import com.aldo.event_pass.entity.Compra;

import jakarta.persistence.LockModeType;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Compra> findByIdAndUsuarioId(Long compraId, Long usuarioId);

    @Modifying(
        flushAutomatically = true,
        clearAutomatically = true
    )
    @Query("""
    UPDATE Compra c
    SET c.estado = EXPIRADA
    WHERE c.estado = RESERVADA
    AND c.fechaExpiracionReserva < CURRENT_TIMESTAMP
    AND NOT EXISTS (
        SELECT p.id
        FROM Pago p
        WHERE p.compra = c
            AND p.proveedor = 'STRIPE'
            AND p.estado = PENDIENTE
    )
    """)
    int expirarReservasVencidas();

    @Modifying(
        flushAutomatically = true,
        clearAutomatically = true
    )
    @Query("""
    UPDATE Compra c
    SET c.estado = CANCELADA
    WHERE c.estado = RESERVADA
    AND EXISTS (
        SELECT 1 FROM DetalleCompra dc
        WHERE dc.compra = c
        AND dc.tipoBoleto.evento.id = :eventoId
    )
    """)
    int cancelarReservasPorEvento(@Param("eventoId") Long eventoId);

    @Modifying(
        flushAutomatically = true,
        clearAutomatically = true
    )
    @Query("""
    UPDATE Compra c
    SET c.estado = EXPIRADA
    WHERE c.estado = RESERVADA
    AND EXISTS (
        SELECT 1 FROM DetalleCompra dc
        WHERE dc.compra = c
        AND dc.tipoBoleto.evento.id = :eventoId
    )
    """)
    int expirarReservasPorEvento(@Param("eventoId") Long eventoId);

    List<Compra> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    @Query("""
    SELECT c.id AS compraId,
           MIN(e.nombre) AS nombreEvento,
           MIN(e.id) AS eventoId,
           c.estado AS estado,
           COALESCE(SUM(dc.cantidad), 0) AS cantidadBoletos,
           c.total AS total,
           c.fechaCreacion AS fechaReserva,
           c.fechaExpiracionReserva AS fechaExpiracion
    FROM Compra c
    LEFT JOIN c.detalles dc
    LEFT JOIN dc.tipoBoleto tb
    LEFT JOIN tb.evento e
    WHERE c.usuario.id = :usuarioId
    GROUP BY c.id, c.estado, c.total, c.fechaCreacion, c.fechaExpiracionReserva
    ORDER BY c.fechaCreacion DESC
    """)
    List<CompraResumenProjection> findResumenByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("""
    SELECT c FROM Compra c
    WHERE c.id = :compraId
    AND c.usuario.id = :usuarioId
    """)
    Optional<Compra> buscarMiCompra(@Param("compraId") Long compraId, @Param("usuarioId") Long usuarioId);

    @Query("""
    SELECT DISTINCT c FROM Compra c
    LEFT JOIN FETCH c.detalles dc
    LEFT JOIN FETCH dc.tipoBoleto tb
    LEFT JOIN FETCH tb.evento e
    WHERE c.id = :compraId
    AND c.usuario.id = :usuarioId
    """)
    Optional<Compra> buscarMiCompraConDetalles(@Param("compraId") Long compraId, @Param("usuarioId") Long usuarioId);
}
