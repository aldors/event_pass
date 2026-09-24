package com.aldo.event_pass.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aldo.event_pass.dto.evento.EventoDetalleResponse;
import com.aldo.event_pass.dto.evento.EventoListadoResponse;
import com.aldo.event_pass.service.interfaces.EventoPublicService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/eventos")
@RequiredArgsConstructor
public class EventoPublicController {

    private final EventoPublicService eventoPublicService;

    @GetMapping("/obtener")
    public ResponseEntity<List<EventoListadoResponse>> obtenerEventos(){
        return ResponseEntity.ok(eventoPublicService.obtenerEventos());
    }

    @GetMapping("/{eventoId}/obtener")
    public ResponseEntity<EventoDetalleResponse> obtenerEventoPorId(@PathVariable Long eventoId){
        return ResponseEntity.ok(eventoPublicService.obtenerEventoPorId(eventoId));
    }
    
}
