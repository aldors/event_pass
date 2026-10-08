package com.aldo.event_pass.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aldo.event_pass.dto.evento.EventoDetalleResponse;
import com.aldo.event_pass.dto.evento.EventoListadoResponse;
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
@PreAuthorize("hasRole('ADMIN')")
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

    @PostMapping("/{eventoId}/cancelar")
    public ResponseEntity<EventoResponse> cancelarEvento(@PathVariable Long eventoId) {
        return ResponseEntity.ok(eventoAdminService.cancelarEvento(eventoId));
    }

    @DeleteMapping("/{eventoId}")
    public ResponseEntity<Void> eliminarBorrador(@PathVariable Long eventoId) {
        eventoAdminService.eliminarBorrador(eventoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/obtener")
    public ResponseEntity<List<EventoListadoResponse>> obtenerEventos() {
        return ResponseEntity.ok(eventoAdminService.obtenerEventos());
    }

    @GetMapping("/{eventoId}/obtener")
    public ResponseEntity<EventoDetalleResponse> obtenerEventoPorId(@PathVariable Long eventoId) {
        return ResponseEntity.ok(eventoAdminService.obtenerEventoPorId(eventoId));
    }
    
}
