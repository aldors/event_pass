package com.aldo.event_pass.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aldo.event_pass.entity.Evento;
import com.aldo.event_pass.enums.EstadoEvento;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {
    
    List<Evento> findByEstadoAndFechaFinAfterOrderByFechaInicioAsc(EstadoEvento estado, LocalDateTime fecha);
    List<Evento> findByEstadoInAndFechaFinAfterOrderByFechaInicioAsc(List<EstadoEvento> estados, LocalDateTime fecha);
    List<Evento> findByEstadoAndFechaFinBefore(EstadoEvento estado, LocalDateTime fecha);
    List<Evento> findByEstadoInAndFechaFinBefore(List<EstadoEvento> estados, LocalDateTime fecha);
    List<Evento> findByEstadoAndFechaInicioLessThanEqualAndFechaFinAfter(EstadoEvento estado, LocalDateTime inicio, LocalDateTime fin);
    List<Evento> findAllByOrderByFechaInicioAsc();
}
