package com.aldo.event_pass.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aldo.event_pass.entity.Boleto;

public interface BoletoRepository extends JpaRepository<Boleto, Long> {
    
    @Query("""
    SELECT COUNT(b)
    FROM Boleto b
    WHERE b.detalleCompra.compra.usuario.id = :usuarioId
    AND b.detalleCompra.tipoBoleto.evento.id = :eventoId
    AND (
        b.detalleCompra.compra.estado = 'PAGADA'
        OR
        (
            b.detalleCompra.compra.estado = 'RESERVADA'
            AND b.detalleCompra.compra.fechaExpiracionReserva > CURRENT_TIMESTAMP
        )
    )
    """)
    Long contarBoletosUsuarioPorEvento(Long usuarioId, Long eventoId);
    Optional<Boleto> findByCodigoQr(String codigoQr);

    @Modifying
    @Query("""
    UPDATE Boleto b
    SET b.estado = 'INVALIDADO'
    WHERE b.estado = 'ACTIVO'
    AND b.detalleCompra.tipoBoleto.evento.id = :eventoId
    """)
    int invalidarBoletosActivosPorEvento(@Param("eventoId") Long eventoId);

    @Modifying
    @Query("""
    UPDATE Boleto b
    SET b.estado = 'INVALIDADO'
    WHERE b.estado = 'ACTIVO'
    AND b.detalleCompra.compra.estado = 'PAGADA'
    AND b.detalleCompra.tipoBoleto.evento.id = :eventoId
    """)
    int invalidarBoletosActivosPagadosPorEvento(@Param("eventoId") Long eventoId);
}
