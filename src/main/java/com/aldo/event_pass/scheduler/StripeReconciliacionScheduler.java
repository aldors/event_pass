package com.aldo.event_pass.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import com.aldo.event_pass.enums.EstadoCompra;
import com.aldo.event_pass.enums.EstadoPago;
import com.aldo.event_pass.repository.PagoRepository;
import com.aldo.event_pass.service.implementaciones.StripeWebhookService;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StripeReconciliacionScheduler {

    private final PagoRepository pagoRepository;
    private final StripeWebhookService stripeWebhookService;
    private final StripeClient stripeClient;

    @Scheduled(
        fixedDelayString = "${app.schedulers.stripe-reconciliation-delay-ms:300000}"
    )
    public void reconciliarPagosPendientes() {
        List<String> sesiones = pagoRepository.buscarSesionesPendientesVencidas(
            "STRIPE",
            EstadoPago.PENDIENTE,
            EstadoCompra.RESERVADA,
            LocalDateTime.now()
        );

        for (String sessionId : sesiones) {
            try {
                reconciliarSesion(sessionId);
            } catch (StripeException | RuntimeException ex) {
                log.warn(
                    "No se pudo reconciliar la sesión de Stripe {}. Se reintentará en la siguiente ejecución.",
                    sessionId,
                    ex
                );
            }
        }
    }

    private void reconciliarSesion(String sessionId) throws StripeException {
        Session session = stripeClient.v1().checkout().sessions().retrieve(sessionId);

        if ("open".equals(session.getStatus())) {
            // La reserva ya venció localmente: intentamos cerrar el Checkout.
            session = stripeClient.v1().checkout().sessions().expire(sessionId);
        }

        stripeWebhookService.procesarSesionReconciliada(session);
    }
}