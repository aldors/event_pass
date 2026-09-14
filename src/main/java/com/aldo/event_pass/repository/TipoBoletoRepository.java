package com.aldo.event_pass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aldo.event_pass.entity.TipoBoleto;

@Repository
public interface TipoBoletoRepository extends JpaRepository<TipoBoleto, Long> {
    
    boolean existsByEventoIdAndNombreIgnoreCase(Long eventoId, String nombre);
    long countByEventoId(Long eventoId);
}
