package com.aldo.event_pass.support;

import java.time.LocalDateTime;

import com.aldo.event_pass.entity.Evento;
import com.aldo.event_pass.enums.EstadoEvento;

import lombok.experimental.UtilityClass;

/* Esta anotación marca la clase como utilitaria haciendo que todos
   sus métodos sean estaticos y no se puede instanciar */

@UtilityClass
public class EventoEstadoResolver {

    public EstadoEvento estadoEfectivo(Evento evento) {
        return estadoEfectivo(evento, LocalDateTime.now());
    }

    public EstadoEvento estadoEfectivo(Evento evento, LocalDateTime ahora) {
        if (evento == null || evento.getEstado() == null) {
            return null;
        }

        EstadoEvento estado = evento.getEstado();

        if (estado == EstadoEvento.CANCELADO || estado == EstadoEvento.BORRADOR || estado == EstadoEvento.FINALIZADO) {
            return estado;
        }

        LocalDateTime inicio = evento.getFechaInicio();
        LocalDateTime fin = evento.getFechaFin();

        if ((estado == EstadoEvento.PUBLICADO || estado == EstadoEvento.INICIADO) && fin != null && !ahora.isBefore(fin)) {
            return EstadoEvento.FINALIZADO;
        }

        if ((estado == EstadoEvento.PUBLICADO || estado == EstadoEvento.INICIADO) && inicio != null && !ahora.isBefore(inicio)) {
            return EstadoEvento.INICIADO;
        }

        return EstadoEvento.PUBLICADO;
    }

}