package com.aldo.event_pass.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aldo.event_pass.entity.Pago;
import com.aldo.event_pass.enums.EstadoCompra;
import com.aldo.event_pass.enums.EstadoPago;

import jakarta.persistence.LockModeType;

public interface PagoRepository extends JpaRepository<Pago, Long>{
    
    Optional<Pago> findFirstByCompraIdAndProveedorAndEstadoOrderByFechaCreacionDesc(
        Long compraId,
        String proveedor,
        EstadoPago estado
    );

    Optional<Pago> findByProveedorAndIdPagoExterno(
        String proveedor,
        String idPagoExterno
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT p
    FROM Pago p
    JOIN FETCH p.compra c
    WHERE p.proveedor = :proveedor
      AND p.idPagoExterno = :idPagoExterno
        """)
    Optional<Pago> buscarPagoConCompraParaWebhook(@Param("proveedor") String proveedor, @Param("idPagoExterno") String idPagoExterno);

    @Query("""
    SELECT p.idPagoExterno
    FROM Pago p
    WHERE p.proveedor = :proveedor
      AND p.estado = :estadoPago
      AND p.idPagoExterno IS NOT NULL
      AND p.compra.estado = :estadoCompra
      AND p.compra.fechaExpiracionReserva < :ahora
    ORDER BY p.fechaCreacion ASC
    """)
    List<String> buscarSesionesPendientesVencidas(
        @Param("proveedor") String proveedor,
        @Param("estadoPago") EstadoPago estadoPago,
        @Param("estadoCompra") EstadoCompra estadoCompra,
        @Param("ahora") LocalDateTime ahora
    );
}
