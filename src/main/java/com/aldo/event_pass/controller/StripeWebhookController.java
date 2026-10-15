package com.aldo.event_pass.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aldo.event_pass.service.implementaciones.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {
    
    private final StripeWebhookService stripeWebhookService;

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    @PostMapping("/stripe")
    public ResponseEntity<String> recibirEvento(@RequestBody String payload,
            @RequestHeader(
                value = "Stripe-Signature",
                required = false
            ) String firma) {

        if (webhookSecret == null
                || webhookSecret.isBlank()
                || firma == null
                || firma.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Firma o configuración del webhook inválida.");
        }

        final Event evento;

        try {
            evento = Webhook.constructEvent(payload, firma, webhookSecret);

        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Firma de Stripe inválida.");

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Evento de Stripe inválido.");
        }

        try {
            stripeWebhookService.procesarEvento(evento);
        } catch (IllegalStateException e) {
            log.warn("Webhook de Stripe no aplicable (se responde 200): {}", e.getMessage());
        }

        return ResponseEntity.ok("Evento procesado.");
    }
}
