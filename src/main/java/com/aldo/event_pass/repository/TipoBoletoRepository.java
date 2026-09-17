package com.aldo.event_pass.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.aldo.event_pass.entity.TipoBoleto;

import jakarta.persistence.LockModeType;

@Repository
public interface TipoBoletoRepository extends JpaRepository<TipoBoleto, Long> {
    
    boolean existsByEventoIdAndNombreIgnoreCase(Long eventoId, String nombre);
    long countByEventoId(Long eventoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT tb
        FROM TipoBoleto tb
        WHERE tb.id IN :ids
    """)
    List<TipoBoleto> buscarTodosParaReserva(@Param("ids") Collection<Long> ids);
}
