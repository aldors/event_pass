package com.aldo.event_pass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aldo.event_pass.entity.DetalleCompra;

public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Long> {
    
    @Query("""
    SELECT COALESCE(SUM(dc.cantidad), 0)
    FROM DetalleCompra dc
    WHERE dc.tipoBoleto.id = :tipoBoletoId
    AND (
        dc.compra.estado = 'PAGADA'
        OR
        (
            dc.compra.estado = 'RESERVADA'
            AND dc.compra.fechaExpiracionReserva > CURRENT_TIMESTAMP
        )
    )
    """)
    Long obtenerBoletosOcupados(@Param("tipoBoletoId") Long tipoBoletoId);
}
