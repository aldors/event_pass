package com.aldo.event_pass.service.implementaciones;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aldo.event_pass.entity.Compra;
import com.aldo.event_pass.entity.Pago;
import com.aldo.event_pass.enums.EstadoCompra;
import com.aldo.event_pass.enums.EstadoPago;
import com.aldo.event_pass.repository.CompraRepository;
import com.aldo.event_pass.repository.PagoRepository;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StripeWebhookService {
    
    private final PagoRepository pagoRepository;
    private final CompraRepository compraRepository;
    private final StripeClient stripeClient;

    @Value("${app.stripe.currency:mxn}")
    private String stripeCurrency;

    @Transactional
    public void procesarSesionReconciliada(Session session) {
        if (session == null || session.getStatus() == null) {
            throw new IllegalStateException("Stripe no devolvió un estado válido para la sesión.");
        }

        switch (session.getStatus()) {
            case "complete" ->
                procesarPagoCompletado(session);

            case "expired" ->
                procesarSesionExpirada(session);

            case "open" ->
                throw new IllegalStateException( "La sesión de Stripe todavía está abierta.");

            default ->
                throw new IllegalStateException("Estado de Checkout no reconocido: " + session.getStatus());
        }
    }

    @Transactional
    public void procesarEvento(Event evento) {

        String tipoEvento = evento.getType();

        if ("checkout.session.completed".equals(tipoEvento)) {

            Session session = obtenerSesionAutorizada(evento);
            procesarPagoCompletado(session);

        } else if ("checkout.session.expired".equals(tipoEvento)) {

            Session session = obtenerSesionAutorizada(evento);
            procesarSesionExpirada(session);

        } else if ("checkout.session.async_payment_succeeded".equals(tipoEvento)) {

            Session session = obtenerSesionAutorizada(evento);
            procesarPagoCompletado(session);

        } else if ("checkout.session.async_payment_failed".equals(tipoEvento)) {

            Session session = obtenerSesionAutorizada(evento);
            procesarPagoFallido(session);
        }

        // Los demás eventos se ignoran (p. ej. charge.refunded:
        // no se guarda el payment_intent, ver limitación documentada).
    }

    private Session obtenerSesionAutorizada(Event evento) {

        Session delEvento = obtenerSesion(evento);

        try {
            return stripeClient.v1().checkout().sessions().retrieve(delEvento.getId());
        } catch (StripeException e) {
            throw new RuntimeException("No fue posible consultar la sesión en Stripe.", e);
        }
    }

    private Session obtenerSesion(Event evento) {

        StripeObject objeto = evento.getDataObjectDeserializer()
            .getObject()
            .orElseThrow(() -> new IllegalStateException("No se pudo interpretar la sesión de Stripe."));

        if (!(objeto instanceof Session)) {
            throw new IllegalStateException("El evento no contiene una sesión de Checkout.");
        }

        return (Session) objeto;
    }

    private void procesarPagoCompletado(Session session) {

        Pago pago = pagoRepository.buscarPagoConCompraParaWebhook("STRIPE", session.getId())
            .orElseThrow(() -> new IllegalStateException("No existe un pago asociado a la sesión de Stripe."));

        Compra compra = pago.getCompra();

        Long compraId = compra.getId();

        // Comprobar que la sesión pertenece a la compra esperada.
        String compraIdMetadata = session.getMetadata() == null ? null : session.getMetadata().get("compraId");

        if (compraIdMetadata == null
                || !compraId.toString().equals(compraIdMetadata)
                || !compraId.toString().equals(session.getClientReferenceId())) {
            throw new IllegalStateException("La sesión no coincide con la compra.");
        }

        // Validar importe y moneda usando los valores de Stripe
        // y el total guardado en la base de datos.
        BigDecimal totalCompra = compra.getTotal();

        long totalEnCentavos = totalCompra.movePointRight(2).longValueExact();

        if (session.getAmountTotal() == null
                || session.getAmountTotal() != totalEnCentavos
                || !stripeCurrency.equalsIgnoreCase(session.getCurrency())) {
            throw new IllegalStateException("El importe o la moneda del pago no coinciden.");
        }

        // Un evento duplicado no debe volver a aprobar el pago.
        if (pago.getEstado() == EstadoPago.APROBADO && compra.getEstado() == EstadoCompra.PAGADA) {
            return;
        }

        if (!"paid".equals(session.getPaymentStatus())) {
            return;
        }

        if (pago.getEstado() != EstadoPago.PENDIENTE || compra.getEstado() != EstadoCompra.RESERVADA) {
            throw new IllegalStateException("La compra ya no está en un estado válido para pagar.");
        }

        pago.setEstado(EstadoPago.APROBADO);
        pago.setFechaConfirmacion(java.time.LocalDateTime.now());

        compra.setEstado(EstadoCompra.PAGADA);
        compra.setFechaPago(java.time.LocalDateTime.now());

        pagoRepository.save(pago);
        compraRepository.save(compra);
    }

    private void procesarSesionExpirada(Session session) {

        Pago pago = pagoRepository.buscarPagoConCompraParaWebhook("STRIPE", session.getId())
            .orElseThrow(() -> new IllegalStateException("No existe un pago asociado a la sesión expirada."));

        Compra compra = pago.getCompra();

        if (pago.getEstado() == EstadoPago.APROBADO || compra.getEstado() == EstadoCompra.PAGADA) {
            return;
        }

        if (pago.getEstado() == EstadoPago.PENDIENTE && compra.getEstado() == EstadoCompra.RESERVADA) {

            pago.setEstado(EstadoPago.EXPIRADO);
            compra.setEstado(EstadoCompra.EXPIRADA);

            pagoRepository.save(pago);
            compraRepository.save(compra);
        }
    }

    private void procesarPagoFallido(Session session) {

        Pago pago = pagoRepository.buscarPagoConCompraParaWebhook("STRIPE", session.getId())
            .orElseThrow(() -> new IllegalStateException("No existe un pago asociado a la sesión fallida."));

        Compra compra = pago.getCompra();

        if (pago.getEstado() == EstadoPago.APROBADO || compra.getEstado() == EstadoCompra.PAGADA) {
            return;
        }

        if (pago.getEstado() == EstadoPago.PENDIENTE && compra.getEstado() == EstadoCompra.RESERVADA) {

            pago.setEstado(EstadoPago.RECHAZADO);
            compra.setEstado(EstadoCompra.EXPIRADA);

            pagoRepository.save(pago);
            compraRepository.save(compra);
        }
    }
}
