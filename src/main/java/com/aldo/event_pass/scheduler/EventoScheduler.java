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
    public void finalizarEventosVencidos() {

        List<Evento> vencidos = eventoRepository.findByEstadoAndFechaFinBefore(EstadoEvento.PUBLICADO, LocalDateTime.now());

        for(Evento evento : vencidos) {
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
