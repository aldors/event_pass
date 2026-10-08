package com.aldo.event_pass.service.interfaces;

import java.util.List;

import com.aldo.event_pass.dto.evento.EventoDetalleResponse;
import com.aldo.event_pass.dto.evento.EventoListadoResponse;
import com.aldo.event_pass.dto.evento.EventoRequest;
import com.aldo.event_pass.dto.evento.EventoResponse;
import com.aldo.event_pass.dto.tipo_boleto.TipoBoletoRequest;
import com.aldo.event_pass.dto.tipo_boleto.TipoBoletoResponse;

public interface EventoAdminService {
    
    public EventoResponse crear(EventoRequest eventoRequest);
    public TipoBoletoResponse agregarTipoBoleto(Long eventoId, TipoBoletoRequest tipoBoletoRequest);
    public EventoResponse publicarEvento(Long eventoId);
    public EventoResponse cancelarEvento(Long eventoId);
    public void eliminarBorrador(Long eventoId);
    public List<EventoListadoResponse> obtenerEventos();
    public EventoDetalleResponse obtenerEventoPorId(Long eventoId);
}
