package com.aldo.event_pass.service.implementaciones;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aldo.event_pass.dto.evento.EventoDetalleResponse;
import com.aldo.event_pass.dto.evento.EventoListadoResponse;
import com.aldo.event_pass.entity.Evento;
import com.aldo.event_pass.enums.EstadoEvento;
import com.aldo.event_pass.mapper.EventoMapper;
import com.aldo.event_pass.repository.EventoRepository;
import com.aldo.event_pass.service.interfaces.EventoPublicService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventoPublicServiceImpl implements EventoPublicService {

    private final EventoRepository eventoRepository;
    
    @Override
    @Transactional(readOnly = true)
    public List<EventoListadoResponse> obtenerEventos() {
        return eventoRepository.findByEstadoOrderByFechaInicioAsc(EstadoEvento.PUBLICADO)
            .stream()
            .map(EventoMapper::toListadoResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EventoDetalleResponse obtenerEventosPorId(Long eventoId) {

        Evento evento = eventoRepository.findById(eventoId)
            .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

        if(evento.getEstado() != EstadoEvento.PUBLICADO) {
            throw new RuntimeException( "Evento no disponible");
        }

        return EventoMapper.toDetalleResponse(evento);
    }
    
}
