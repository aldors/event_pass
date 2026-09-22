package com.aldo.event_pass.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.aldo.event_pass.dto.evento.EventoDetalleResponse;
import com.aldo.event_pass.dto.evento.EventoListadoResponse;
import com.aldo.event_pass.dto.evento.EventoRequest;
import com.aldo.event_pass.dto.evento.EventoResponse;
import com.aldo.event_pass.dto.tipo_boleto.TipoBoletoDisponibleResponse;
import com.aldo.event_pass.entity.Evento;

@Component
public class EventoMapper {
    
    public static Evento toEntity(EventoRequest eventoRequest){
        return Evento.builder()
            .nombre(eventoRequest.getNombre())
            .descripcion(eventoRequest.getDescripcion())
            .fechaInicio(eventoRequest.getFechaInicio())
            .fechaFin(eventoRequest.getFechaFin())
            .ubicacion(eventoRequest.getUbicacion())
            .maxBoletosPorUsuario(eventoRequest.getMaxBoletosPorUsuario())
            .build();
    }

    public static EventoResponse toResponse(Evento evento){
        return new EventoResponse(
            evento.getId(),
            evento.getNombre(),
            evento.getDescripcion(),
            evento.getUbicacion(),
            evento.getFechaInicio(),
            evento.getFechaFin(),
            evento.getMaxBoletosPorUsuario(),
            evento.getEstado()
        );
    }

    public static EventoListadoResponse toListadoResponse(Evento evento){
        return new EventoListadoResponse(
            evento.getId(),
            evento.getNombre(),
            evento.getUbicacion(),
            evento.getFechaInicio(),
            evento.getEstado()
        );
    }

    public static EventoDetalleResponse toDetalleResponse(Evento evento, List<TipoBoletoDisponibleResponse> tiposBoleto){
        return new EventoDetalleResponse(
            evento.getId(),
            evento.getNombre(),
            evento.getDescripcion(),
            evento.getUbicacion(),
            evento.getFechaInicio(),
            evento.getFechaFin(),
            evento.getMaxBoletosPorUsuario(),
            tiposBoleto
        );
    }
}
