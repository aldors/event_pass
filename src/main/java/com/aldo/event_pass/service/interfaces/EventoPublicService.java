package com.aldo.event_pass.service.interfaces;

import java.util.List;

import com.aldo.event_pass.dto.evento.EventoDetalleResponse;
import com.aldo.event_pass.dto.evento.EventoListadoResponse;

public interface EventoPublicService {
    
    public List<EventoListadoResponse> obtenerEventos();
    public EventoDetalleResponse obtenerEventoPorId(Long eventoId);
}
