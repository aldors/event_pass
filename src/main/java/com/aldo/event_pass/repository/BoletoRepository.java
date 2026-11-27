package com.aldo.event_pass.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aldo.event_pass.dto.projection.BoletoDetalleProjection;
import com.aldo.event_pass.entity.Boleto;

public interface BoletoRepository extends JpaRepository<Boleto, Long> {
    
    @Query("""
    SELECT COUNT(b)
    FROM Boleto b
    WHERE b.detalleCompra.compra.usuario.id = :usuarioId
    AND b.detalleCompra.tipoBoleto.evento.id = :eventoId
    AND (
        b.detalleCompra.compra.estado = PAGADA
        OR
        (
            b.detalleCompra.compra.estado = RESERVADA
            AND b.detalleCompra.compra.fechaExpiracionReserva > CURRENT_TIMESTAMP
        )
    )
    """)
    Long contarBoletosUsuarioPorEvento(Long usuarioId, Long eventoId);
    Optional<Boleto> findByCodigoQr(String codigoQr);

    @Query("""
    SELECT DISTINCT b FROM Boleto b
    JOIN FETCH b.detalleCompra dc
    JOIN FETCH dc.compra c
    JOIN FETCH c.usuario u
    JOIN FETCH dc.tipoBoleto tb
    JOIN FETCH tb.evento e
    WHERE b.id = :id
    """)
    Optional<Boleto> findByIdForPdf(@Param("id") Long id);

    @Query("""
    SELECT b.id AS boletoId,
           tb.nombre AS tipoBoletoNombre,
           b.titularNombre AS titularNombre,
           b.folio AS folio,
           b.estado AS estado
    FROM Boleto b
    JOIN b.detalleCompra dc
    JOIN dc.tipoBoleto tb
    WHERE dc.compra.id = :compraId
    """)
    List<BoletoDetalleProjection> findDetallesByCompraId(@Param("compraId") Long compraId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
    UPDATE Boleto b
    SET b.estado = INVALIDADO
    WHERE b.estado = ACTIVO
    AND b.detalleCompra.tipoBoleto.evento.id = :eventoId
    """)
    int invalidarBoletosActivosPorEvento(@Param("eventoId") Long eventoId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
    UPDATE Boleto b
    SET b.estado = INVALIDADO
    WHERE b.estado = ACTIVO
    AND b.detalleCompra.compra.estado = PAGADA
    AND b.detalleCompra.tipoBoleto.evento.id = :eventoId
    """)
    int invalidarBoletosActivosPagadosPorEvento(@Param("eventoId") Long eventoId);
}
