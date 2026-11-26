package com.aldo.event_pass.service.implementaciones;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aldo.event_pass.dto.pago.CheckoutResponse;
import com.aldo.event_pass.entity.Compra;
import com.aldo.event_pass.entity.Pago;
import com.aldo.event_pass.enums.EstadoCompra;
import com.aldo.event_pass.enums.EstadoPago;
import com.aldo.event_pass.exception.CompraNoEncontradaException;
import com.aldo.event_pass.exception.CompraNoReservadaException;
import com.aldo.event_pass.exception.CompraNoValidaParaPagarException;
import com.aldo.event_pass.exception.CompraYaPagadaException;
import com.aldo.event_pass.exception.ImporteDePagoNoCoincidenteException;
import com.aldo.event_pass.exception.ReservaExpiradaException;
import com.aldo.event_pass.exception.SesionAbiertaException;
import com.aldo.event_pass.exception.SesionDePagoInvalidaException;
import com.aldo.event_pass.exception.SesionDePagoNoAsociadaException;
import com.aldo.event_pass.exception.SesionNoCoincideConLaCompraException;
import com.aldo.event_pass.repository.CompraRepository;
import com.aldo.event_pass.repository.PagoRepository;
import com.stripe.model.checkout.Session;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PagoTransaccionService {

    private final PagoRepository pagoRepository;
    private final CompraRepository compraRepository;

    @Value("${app.stripe.currency:mxn}")
    private String stripeCurrency;

    @Transactional
    public void aplicarSesion(Session session) {

        if (session == null || session.getStatus() == null) {
            throw new SesionDePagoInvalidaException();
        }

        switch (session.getStatus()) {
            case "complete" -> procesarPagoCompletado(session);
            case "expired" -> procesarSesionExpirada(session);
            case "open" -> throw new SesionAbiertaException();
            default ->
                throw new SesionDePagoInvalidaException(session.getStatus());
        }
    }

    @Transactional
    public void aplicarEvento(String tipoEvento, Session session) {

        switch (tipoEvento) {
            case "checkout.session.completed", "checkout.session.async_payment_succeeded" -> 
            procesarPagoCompletado(session);

            case "checkout.session.expired" ->
            procesarSesionExpirada(session);

            case "checkout.session.async_payment_failed" ->
            procesarPagoFallido(session);

            default -> {
            }
        }
    }

    @Transactional
    public CheckoutResponse registrarSesion(
        Long compraId,
        Long usuarioId,
        Session session,
        int minutosExpiracion
    ) {

        Compra compra = compraRepository.findByIdAndUsuarioId(compraId, usuarioId)
            .orElseThrow(() -> new CompraNoEncontradaException());

        validarReserva(compra);

        Pago pago = Pago.builder()
            .compra(compra)
            .proveedor("STRIPE")
            .idPagoExterno(session.getId())
            .urlCheckout(session.getUrl())
            .monto(compra.getTotal())
            .estado(EstadoPago.PENDIENTE)
            .build();

        pagoRepository.save(pago);

        compra.setFechaExpiracionReserva(LocalDateTime.now().plusMinutes(minutosExpiracion));
        compraRepository.save(compra);

        return new CheckoutResponse(
            compra.getId(),
            pago.getEstado().name(),
            pago.getUrlCheckout()
        );
    }

    private void validarReserva(Compra compra) {

        if (compra.getEstado() == EstadoCompra.PAGADA) {
            throw new CompraYaPagadaException();
        }

        if (compra.getEstado() != EstadoCompra.RESERVADA) {
            throw new CompraNoReservadaException();
        }

        if (compra.getFechaExpiracionReserva().isBefore(LocalDateTime.now())) {
            throw new ReservaExpiradaException();
        }
    }

    private void procesarPagoCompletado(Session session) {

        Pago pago = pagoRepository.buscarPagoConCompraParaWebhook("STRIPE", session.getId())
            .orElseThrow(() -> new SesionDePagoNoAsociadaException());

        Compra compra = pago.getCompra();

        Long compraId = compra.getId();

        String compraIdMetadata = session.getMetadata() == null ? null : session.getMetadata().get("compraId");

        if (compraIdMetadata == null
                || !compraId.toString().equals(compraIdMetadata)
                || !compraId.toString().equals(session.getClientReferenceId())) {
            throw new SesionNoCoincideConLaCompraException();
        }

        BigDecimal totalCompra = compra.getTotal();

        long totalEnCentavos = totalCompra.movePointRight(2).longValueExact();

        if (session.getAmountTotal() == null
                || session.getAmountTotal() != totalEnCentavos
                || !stripeCurrency.equalsIgnoreCase(session.getCurrency())) {
            throw new ImporteDePagoNoCoincidenteException();
        }

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
        pago.setFechaConfirmacion(LocalDateTime.now());

        compra.setEstado(EstadoCompra.PAGADA);
        compra.setFechaPago(LocalDateTime.now());

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

            if (compra.getFechaExpiracionReserva().isBefore(LocalDateTime.now())) {
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
