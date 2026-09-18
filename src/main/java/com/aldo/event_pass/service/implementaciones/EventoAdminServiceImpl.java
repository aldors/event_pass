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
import com.aldo.event_pass.exception.AlMenosUnBoletoException;
import com.aldo.event_pass.exception.BoletoExistenteException;
import com.aldo.event_pass.exception.EventoNoEncontradoException;
import com.aldo.event_pass.exception.EventosEnBorradorException;
import com.aldo.event_pass.exception.FechaFinPosteriorAInicioException;
import com.aldo.event_pass.exception.FechaInicioFuturaException;
import com.aldo.event_pass.exception.PublicarEventoException;
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
            throw new FechaFinPosteriorAInicioException();
        }

        Evento evento = EventoMapper.toEntity(eventoRequest);
        return EventoMapper.toResponse(eventoRepository.save(evento));
    }

    @Override
    @Transactional
    public TipoBoletoResponse agregarTipoBoleto(Long eventoId, TipoBoletoRequest tipoBoletoRequest) {
        
        Evento evento = eventoRepository.findById(eventoId)
            .orElseThrow(() -> new EventoNoEncontradoException());

        if(evento.getEstado() != EstadoEvento.BORRADOR){
            throw new EventosEnBorradorException();
        }

        if(tipoBoletoRepository.existsByEventoIdAndNombreIgnoreCase(eventoId, tipoBoletoRequest.getNombre())){
            throw new BoletoExistenteException();
        }

        TipoBoleto tipoBoleto = TipoBoletoMapper.toEntity(tipoBoletoRequest);
        tipoBoleto.setEvento(evento);

        return TipoBoletoMapper.toResponse(tipoBoletoRepository.save(tipoBoleto));
    }

    @Override
    @Transactional
    public EventoResponse publicarEvento(Long eventoId) {
        
        Evento evento = eventoRepository.findById(eventoId)
            .orElseThrow(() -> new EventoNoEncontradoException());

        if(evento.getEstado() != EstadoEvento.BORRADOR) {
            throw new PublicarEventoException();
        }

        long cantidadTiposBoleto = tipoBoletoRepository.countByEventoId(eventoId);

        if(cantidadTiposBoleto == 0) {
            throw new AlMenosUnBoletoException();
        }

        if(!evento.getFechaInicio().isAfter(LocalDateTime.now())) {
            throw new FechaInicioFuturaException();
        }

        evento.setEstado(EstadoEvento.PUBLICADO);

        return EventoMapper.toResponse(eventoRepository.save(evento));
    }
    
}
