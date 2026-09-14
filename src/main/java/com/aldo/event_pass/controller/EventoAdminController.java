package com.aldo.event_pass.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aldo.event_pass.dto.evento.EventoRequest;
import com.aldo.event_pass.dto.evento.EventoResponse;
import com.aldo.event_pass.dto.tipo_boleto.TipoBoletoRequest;
import com.aldo.event_pass.dto.tipo_boleto.TipoBoletoResponse;
import com.aldo.event_pass.service.interfaces.EventoAdminService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/eventos")
@RequiredArgsConstructor
public class EventoAdminController {

    private final EventoAdminService eventoAdminService;

    @PostMapping("/crear")
    public ResponseEntity<EventoResponse> crear(@Valid @RequestBody EventoRequest eventoRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoAdminService.crear(eventoRequest));
    }

    @PostMapping("/{eventoId}/agregar-tipo-boleto")
    public ResponseEntity<TipoBoletoResponse> agregarTipoBoleto(@PathVariable Long eventoId, @Valid @RequestBody TipoBoletoRequest tipoBoletoRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoAdminService.agregarTipoBoleto(eventoId, tipoBoletoRequest));
    }

    @PostMapping("/{eventoId}/publicar")
    public ResponseEntity<EventoResponse> publicarEvento(@PathVariable Long eventoId) {
        return ResponseEntity.ok(eventoAdminService.publicarEvento(eventoId));
    }
    
}
