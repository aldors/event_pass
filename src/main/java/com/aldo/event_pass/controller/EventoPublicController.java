package com.aldo.event_pass.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aldo.event_pass.dto.evento.EventoDetalleResponse;
import com.aldo.event_pass.dto.evento.EventoListadoResponse;
import com.aldo.event_pass.exception.ApiError.ApiError;
import com.aldo.event_pass.service.interfaces.EventoPublicService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Eventos públicos", description = "Catálogo público de eventos para compra")
@RestController
@RequestMapping("/eventos")
@RequiredArgsConstructor
public class EventoPublicController {

    private final EventoPublicService eventoPublicService;

    @Operation(summary = "Listar eventos", description = "Devuelve los eventos publicados disponibles. Endpoint público.", security = {})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado obtenido",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = EventoListadoResponse.class))))
    })
    @GetMapping("/obtener")
    public ResponseEntity<List<EventoListadoResponse>> obtenerEventos(){
        return ResponseEntity.ok(eventoPublicService.obtenerEventos());
    }

    @Operation(summary = "Ver detalle de evento", description = "Devuelve el detalle de un evento con sus tipos de boleto. Endpoint público.", security = {})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Detalle obtenido",
            content = @Content(schema = @Schema(implementation = EventoDetalleResponse.class))),
        @ApiResponse(responseCode = "404", description = "Evento no encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 404", value = "{\"status\":404,\"message\":\"Recurso no encontrado\",\"timestamp\":\"2026-10-08T10:00:00\",\"errors\":[\"Recurso no encontrado\"]}")))
    })
    @GetMapping("/{eventoId}/obtener")
    public ResponseEntity<EventoDetalleResponse> obtenerEventoPorId(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId){
        return ResponseEntity.ok(eventoPublicService.obtenerEventoPorId(eventoId));
    }
    
}
