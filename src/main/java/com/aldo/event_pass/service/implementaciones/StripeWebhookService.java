package com.aldo.event_pass.service.implementaciones;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aldo.event_pass.entity.Compra;
import com.aldo.event_pass.entity.Pago;
import com.aldo.event_pass.enums.EstadoCompra;
import com.aldo.event_pass.enums.EstadoPago;
import com.aldo.event_pass.exception.CompraNoValidaParaPagarException;
import com.aldo.event_pass.exception.EventoDeWebhookNoAplicableException;
import com.aldo.event_pass.exception.ImporteDePagoNoCoincidenteException;
import com.aldo.event_pass.exception.SesionAbiertaException;
import com.aldo.event_pass.exception.SesionDePagoInvalidaException;
import com.aldo.event_pass.exception.SesionDePagoNoAsociadaException;
import com.aldo.event_pass.exception.SesionNoCoincideConLaCompraException;
import com.aldo.event_pass.exception.StripeNoDisponibleException;
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
            throw new SesionDePagoInvalidaException();
        }

        switch (session.getStatus()) {
            case "complete" ->
                procesarPagoCompletado(session);

            case "expired" ->
                procesarSesionExpirada(session);

            case "open" ->
                throw new SesionAbiertaException();

            default ->
                throw new SesionDePagoInvalidaException(session.getStatus());
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
            throw new StripeNoDisponibleException("No fue posible consultar la sesión en Stripe.", e);
        }
    }

    private Session obtenerSesion(Event evento) {

        StripeObject objeto = evento.getDataObjectDeserializer()
            .getObject()
            .orElseThrow(() -> new EventoDeWebhookNoAplicableException());

        if (!(objeto instanceof Session)) {
            throw new EventoDeWebhookNoAplicableException();
        }

        return (Session) objeto;
    }

    private void procesarPagoCompletado(Session session) {

        Pago pago = pagoRepository.buscarPagoConCompraParaWebhook("STRIPE", session.getId())
            .orElseThrow(() -> new SesionDePagoNoAsociadaException());

        Compra compra = pago.getCompra();

        Long compraId = compra.getId();

        // Comprobar que la sesión pertenece a la compra esperada.
        String compraIdMetadata = session.getMetadata() == null ? null : session.getMetadata().get("compraId");

        if (compraIdMetadata == null
                || !compraId.toString().equals(compraIdMetadata)
                || !compraId.toString().equals(session.getClientReferenceId())) {
            throw new SesionNoCoincideConLaCompraException();
        }

        // Validar importe y moneda usando los valores de Stripe
        // y el total guardado en la base de datos.
        BigDecimal totalCompra = compra.getTotal();

        long totalEnCentavos = totalCompra.movePointRight(2).longValueExact();

        if (session.getAmountTotal() == null
                || session.getAmountTotal() != totalEnCentavos
                || !stripeCurrency.equalsIgnoreCase(session.getCurrency())) {
            throw new ImporteDePagoNoCoincidenteException();
        }

        // Un evento duplicado no debe volver a aprobar el pago.
        if (pago.getEstado() == EstadoPago.APROBADO && compra.getEstado() == EstadoCompra.PAGADA) {
            return;
        }

        if (!"paid".equals(session.getPaymentStatus())) {
            return;
        }

        if (pago.getEstado() != EstadoPago.PENDIENTE || compra.getEstado() != EstadoCompra.RESERVADA) {
            throw new CompraNoValidaParaPagarException();
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
            .orElseThrow(() -> new SesionDePagoNoAsociadaException());

        Compra compra = pago.getCompra();

        if (pago.getEstado() == EstadoPago.APROBADO || compra.getEstado() == EstadoCompra.PAGADA) {
            return;
        }

        if (pago.getEstado() == EstadoPago.PENDIENTE && compra.getEstado() == EstadoCompra.RESERVADA) {

            pago.setEstado(EstadoPago.EXPIRADO);

            // La compra solo expira si su reserva también venció: si la
            // sesión murió antes (p. ej. expiración manual), la reserva
            // sigue vigente y el usuario puede pagar de nuevo.
            if (compra.getFechaExpiracionReserva().isBefore(java.time.LocalDateTime.now())) {
                compra.setEstado(EstadoCompra.EXPIRADA);
            }

            pagoRepository.save(pago);
            compraRepository.save(compra);
        }
    }

    private void procesarPagoFallido(Session session) {

        Pago pago = pagoRepository.buscarPagoConCompraParaWebhook("STRIPE", session.getId())
            .orElseThrow(() -> new SesionDePagoNoAsociadaException());

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
