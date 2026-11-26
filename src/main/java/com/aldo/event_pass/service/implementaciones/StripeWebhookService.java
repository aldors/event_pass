package com.aldo.event_pass.service.implementaciones;

import org.springframework.stereotype.Service;

import com.aldo.event_pass.exception.EventoDeWebhookNoAplicableException;
import com.aldo.event_pass.exception.StripeNoDisponibleException;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StripeWebhookService {

    private final StripeClient stripeClient;
    private final PagoTransaccionService pagoTransaccionService;

    public void procesarSesionReconciliada(Session session) {
        pagoTransaccionService.aplicarSesion(session);
    }

    public void procesarEvento(Event evento) {

        String tipoEvento = evento.getType();

        if ("checkout.session.completed".equals(tipoEvento)
                || "checkout.session.expired".equals(tipoEvento)
                || "checkout.session.async_payment_succeeded".equals(tipoEvento)
                || "checkout.session.async_payment_failed".equals(tipoEvento)) {

            Session session = obtenerSesionAutorizada(evento);
            pagoTransaccionService.aplicarEvento(tipoEvento, session);
        }
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
}
