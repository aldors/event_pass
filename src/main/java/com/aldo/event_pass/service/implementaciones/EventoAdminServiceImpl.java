package com.aldo.event_pass.service.implementaciones;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aldo.event_pass.dto.evento.EventoRequest;
import com.aldo.event_pass.dto.evento.EventoResponse;
import com.aldo.event_pass.dto.tipo_boleto.TipoBoletoRequest;
import com.aldo.event_pass.dto.tipo_boleto.TipoBoletoResponse;
import com.aldo.event_pass.entity.Evento;
import com.aldo.event_pass.entity.TipoBoleto;
import com.aldo.event_pass.enums.EstadoEvento;
import com.aldo.event_pass.mapper.EventoMapper;
import com.aldo.event_pass.mapper.TipoBoletoMapper;
import com.aldo.event_pass.repository.EventoRepository;
import com.aldo.event_pass.repository.TipoBoletoRepository;
import com.aldo.event_pass.service.interfaces.EventoAdminService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventoAdminServiceImpl implements EventoAdminService {

    private final EventoRepository eventoRepository;
    private final TipoBoletoRepository tipoBoletoRepository;

    @Override
    @Transactional
    public EventoResponse crear(EventoRequest eventoRequest) {

        if(eventoRequest.getFechaFin().isBefore(eventoRequest.getFechaInicio())){
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio");
        }

        Evento evento = EventoMapper.toEntity(eventoRequest);
        return EventoMapper.toResponse(eventoRepository.save(evento));
    }

    @Override
    @Transactional
    public TipoBoletoResponse agregarTipoBoleto(Long eventoId, TipoBoletoRequest tipoBoletoRequest) {
        
        Evento evento = eventoRepository.findById(eventoId)
            .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

        if(evento.getEstado() != EstadoEvento.BORRADOR){
            throw new RuntimeException("Solo se pueden agregar tipoS de boleto a eventos en borrador");
        }

        if(tipoBoletoRepository.existsByEventoIdAndNombreIgnoreCase(eventoId, tipoBoletoRequest.getNombre())){
            throw new RuntimeException("Ya existe un tipo de boleto con este nombre");
        }

        TipoBoleto tipoBoleto = TipoBoletoMapper.toEntity(tipoBoletoRequest);
        tipoBoleto.setEvento(evento);

        return TipoBoletoMapper.toResponse(tipoBoletoRepository.save(tipoBoleto));
    }

    @Override
    @Transactional
    public EventoResponse publicarEvento(Long eventoId) {
        
        Evento evento = eventoRepository.findById(eventoId)
            .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

        if(evento.getEstado() != EstadoEvento.BORRADOR) {
            throw new RuntimeException("Solo los eventos en borrador pueden publicarse");
        }

        long cantidadTiposBoleto = tipoBoletoRepository.countByEventoId(eventoId);

        if(cantidadTiposBoleto == 0) {
            throw new RuntimeException("El evento debe tener al menos un tipo de boleto");
        }

        if(!evento.getFechaInicio().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("La fecha del evento debe ser futura");
        }

        evento.setEstado(EstadoEvento.PUBLICADO);

        return EventoMapper.toResponse(eventoRepository.save(evento));
    }
    
}
