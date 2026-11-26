package com.aldo.event_pass.service.implementaciones;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aldo.event_pass.dto.evento.EventoDetalleResponse;
import com.aldo.event_pass.dto.evento.EventoListadoResponse;
import com.aldo.event_pass.dto.projection.OcupadosPorTipoBoletoProjection;
import com.aldo.event_pass.dto.tipo_boleto.TipoBoletoDisponibleResponse;
import com.aldo.event_pass.entity.Evento;
import com.aldo.event_pass.enums.EstadoEvento;
import com.aldo.event_pass.exception.EventoNoDisponibleException;
import com.aldo.event_pass.exception.EventoNoEncontradoException;
import com.aldo.event_pass.mapper.EventoMapper;
import com.aldo.event_pass.mapper.TipoBoletoMapper;
import com.aldo.event_pass.repository.DetalleCompraRepository;
import com.aldo.event_pass.repository.EventoRepository;
import com.aldo.event_pass.service.interfaces.EventoPublicService;
import com.aldo.event_pass.support.EventoEstadoResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventoPublicServiceImpl implements EventoPublicService {

    private final EventoRepository eventoRepository;
    private final DetalleCompraRepository detalleCompraRepository;
    
    @Override
    @Transactional(readOnly = true)
    public List<EventoListadoResponse> obtenerEventos() {

        LocalDateTime ahora = LocalDateTime.now();

        return eventoRepository.findByEstadoInAndFechaFinAfterOrderByFechaInicioAsc(
                List.of(EstadoEvento.PUBLICADO, EstadoEvento.INICIADO),
                ahora
            )
            .stream()
            .map(EventoMapper::toListadoResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EventoDetalleResponse obtenerEventoPorId(Long eventoId) {

        Evento evento = eventoRepository.findByIdConTiposBoleto(eventoId)
            .orElseThrow(() -> new EventoNoEncontradoException());

        EstadoEvento estadoEfectivo = EventoEstadoResolver.estadoEfectivo(evento);
        
        if (estadoEfectivo != EstadoEvento.PUBLICADO && estadoEfectivo != EstadoEvento.INICIADO) {
            throw new EventoNoDisponibleException();
        }

        Map<Long, Long> ocupadosPorTipo = detalleCompraRepository.obtenerOcupadosPorEvento(eventoId)
            .stream()
            .collect(Collectors.toMap(
                OcupadosPorTipoBoletoProjection::getTipoBoletoId,
                OcupadosPorTipoBoletoProjection::getOcupados
            ));

        List<TipoBoletoDisponibleResponse> tiposBoleto = evento.getTiposBoleto()
            .stream()
            .map(tipoBoleto -> {
                long ocupados = ocupadosPorTipo.getOrDefault(tipoBoleto.getId(), 0L);
                int disponibles = (int) Math.max(0L, tipoBoleto.getCantidadTotal().longValue() - ocupados);
                return TipoBoletoMapper.toDisponibleResponse(tipoBoleto, disponibles);
            })
            .toList();

        return EventoMapper.toDetalleResponse(evento, tiposBoleto);
    }
    
}
