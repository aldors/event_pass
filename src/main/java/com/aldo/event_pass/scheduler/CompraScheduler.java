package com.aldo.event_pass.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.aldo.event_pass.repository.CompraRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CompraScheduler {

    private final CompraRepository compraRepository;

    @Scheduled(fixedDelayString = "${app.schedulers.compra-expiracion-delay-ms:600000}")
    @Transactional
    public void expirarReservasVencidas() {
        int actualizadas = compraRepository.expirarReservasVencidas();

        if (actualizadas > 0) {
            log.info("Reservas expiradas: {}", actualizadas);
        }
    }
}
