package com.aldo.event_pass.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.aldo.event_pass.entity.Evento;
import com.aldo.event_pass.enums.EstadoEvento;
import com.aldo.event_pass.repository.BoletoRepository;
import com.aldo.event_pass.repository.CompraRepository;
import com.aldo.event_pass.repository.EventoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventoScheduler {

    private final EventoRepository eventoRepository;
    private final BoletoRepository boletoRepository;
    private final CompraRepository compraRepository;

    @Scheduled(fixedDelayString = "${app.schedulers.evento-finalizacion-delay-ms:1800000}")
    @Transactional
    public void actualizarEstadosEventos() {
        
        LocalDateTime ahora = LocalDateTime.now();

        List<Evento> porIniciar = eventoRepository
            .findByEstadoAndFechaInicioLessThanEqualAndFechaFinAfter(EstadoEvento.PUBLICADO, ahora, ahora);

        for (Evento evento : porIniciar) {
            evento.setEstado(EstadoEvento.INICIADO);
            eventoRepository.save(evento);
            log.info("Evento {} marcado como INICIADO", evento.getId());
        }

        List<Evento> vencidos = eventoRepository.findByEstadoInAndFechaFinBefore(
            List.of(EstadoEvento.PUBLICADO, EstadoEvento.INICIADO),
            ahora
        );

        for (Evento evento : vencidos) {
            evento.setEstado(EstadoEvento.FINALIZADO);
            eventoRepository.save(evento);

            // No asistieron: ACTIVO + PAGADA -> INVALIDADO. UTILIZADO se mantiene.
            int invalidados = boletoRepository.invalidarBoletosActivosPagadosPorEvento(evento.getId());

            // Reservas colgadas del evento finalizado.
            int expiradas = compraRepository.expirarReservasPorEvento(evento.getId());

            log.info("Evento {} finalizado. Boletos invalidados: {}, reservas expiradas: {}", evento.getId(), invalidados, expiradas);
        }
    }
}
