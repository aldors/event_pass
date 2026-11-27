package com.aldo.event_pass.support;

import com.aldo.event_pass.entity.Compra;
import com.aldo.event_pass.entity.Evento;
import com.aldo.event_pass.enums.EstadoBoleto;
import com.aldo.event_pass.enums.EstadoCompra;
import com.aldo.event_pass.enums.EstadoEvento;
import com.aldo.event_pass.exception.BoletoInvalidadoException;
import com.aldo.event_pass.exception.BoletoNoPagadoException;
import com.aldo.event_pass.exception.BoletoUtilizadoException;
import com.aldo.event_pass.exception.EventoCanceladoException;
import com.aldo.event_pass.exception.EventoFinalizadoException;
import com.aldo.event_pass.exception.EventoNoDisponibleException;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BoletoValidezResolver {

    public boolean esValido(Compra compra, Evento evento, EstadoBoleto estadoBoleto) {
        if (compra == null || compra.getEstado() != EstadoCompra.PAGADA) {
            return false;
        }

        if (estadoBoleto != EstadoBoleto.ACTIVO) {
            return false;
        }

        EstadoEvento estadoEfectivo = EventoEstadoResolver.estadoEfectivo(evento);

        return estadoEfectivo == EstadoEvento.PUBLICADO || estadoEfectivo == EstadoEvento.INICIADO;
    }

    public void validar(Compra compra, Evento evento, EstadoBoleto estadoBoleto) {
        if (estadoBoleto == EstadoBoleto.UTILIZADO) {
            throw new BoletoUtilizadoException();
        }

        if (estadoBoleto == EstadoBoleto.INVALIDADO || estadoBoleto == null) {
            throw new BoletoInvalidadoException();
        }

        if (compra == null || compra.getEstado() != EstadoCompra.PAGADA) {
            throw new BoletoNoPagadoException();
        }

        EstadoEvento estadoEfectivo = EventoEstadoResolver.estadoEfectivo(evento);

        if (estadoEfectivo == EstadoEvento.CANCELADO) {
            throw new EventoCanceladoException();
        }

        if (estadoEfectivo == EstadoEvento.FINALIZADO) {
            throw new EventoFinalizadoException();
        }

        if (estadoEfectivo != EstadoEvento.PUBLICADO && estadoEfectivo != EstadoEvento.INICIADO) {
            throw new EventoNoDisponibleException();
        }
    }
}
