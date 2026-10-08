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
import com.aldo.event_pass.exception.ApiError.ApiError;
import com.aldo.event_pass.service.interfaces.EventoAdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Administración de eventos", description = "Gestión de eventos y tipos de boleto. Solo ADMIN.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/admin/eventos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EventoAdminController {

    private final EventoAdminService eventoAdminService;

    @Operation(summary = "Crear evento", description = "Crea un evento en estado borrador. Requiere rol ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Evento creado",
            content = @Content(schema = @Schema(implementation = EventoResponse.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 400", value = "{\"status\":400,\"message\":\"Error de validación\",\"timestamp\":\"2026-10-08T10:00:00\",\"errors\":[\"La solicitud contiene datos inválidos\"]}"))),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Se requiere rol ADMIN")
    })
    @PostMapping("/crear")
    public ResponseEntity<EventoResponse> crear(@Valid @RequestBody EventoRequest eventoRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoAdminService.crear(eventoRequest));
    }

    @Operation(summary = "Agregar tipo de boleto", description = "Agrega un tipo de boleto (precio, stock) a un evento. Requiere rol ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tipo de boleto creado",
            content = @Content(schema = @Schema(implementation = TipoBoletoResponse.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 400", value = "{\"status\":400,\"message\":\"Error de validación\",\"timestamp\":\"2026-10-08T10:00:00\",\"errors\":[\"La solicitud contiene datos inválidos\"]}"))),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Se requiere rol ADMIN"),
        @ApiResponse(responseCode = "404", description = "Evento no encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 404", value = "{\"status\":404,\"message\":\"Recurso no encontrado\",\"timestamp\":\"2026-10-08T10:00:00\",\"errors\":[\"Recurso no encontrado\"]}")))
    })
    @PostMapping("/{eventoId}/agregar-tipo-boleto")
    public ResponseEntity<TipoBoletoResponse> agregarTipoBoleto(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId,
            @Valid @RequestBody TipoBoletoRequest tipoBoletoRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoAdminService.agregarTipoBoleto(eventoId, tipoBoletoRequest));
    }

    @Operation(summary = "Publicar evento", description = "Cambia un evento de borrador a publicado. Requiere rol ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Evento publicado",
            content = @Content(schema = @Schema(implementation = EventoResponse.class))),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Se requiere rol ADMIN"),
        @ApiResponse(responseCode = "404", description = "Evento no encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 404", value = "{\"status\":404,\"message\":\"Recurso no encontrado\",\"timestamp\":\"2026-10-08T10:00:00\",\"errors\":[\"Recurso no encontrado\"]}"))),
        @ApiResponse(responseCode = "409", description = "El evento no está en estado publicable",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 409", value = "{\"status\":409,\"message\":\"Conflicto\",\"timestamp\":\"2026-10-08T10:00:00\",\"errors\":[\"Conflicto con el estado actual\"]}")))
    })
    @PostMapping("/{eventoId}/publicar")
    public ResponseEntity<EventoResponse> publicarEvento(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId) {
        return ResponseEntity.ok(eventoAdminService.publicarEvento(eventoId));
    }

    @Operation(summary = "Cancelar evento", description = "Cancela un evento publicado. Requiere rol ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Evento cancelado",
            content = @Content(schema = @Schema(implementation = EventoResponse.class))),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Se requiere rol ADMIN"),
        @ApiResponse(responseCode = "404", description = "Evento no encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 404", value = "{\"status\":404,\"message\":\"Recurso no encontrado\",\"timestamp\":\"2026-10-08T10:00:00\",\"errors\":[\"Recurso no encontrado\"]}")))
    })
    @PostMapping("/{eventoId}/cancelar")
    public ResponseEntity<EventoResponse> cancelarEvento(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId) {
        return ResponseEntity.ok(eventoAdminService.cancelarEvento(eventoId));
    }

    @Operation(summary = "Eliminar borrador", description = "Elimina un evento en estado borrador. Requiere rol ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Borrador eliminado"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Se requiere rol ADMIN"),
        @ApiResponse(responseCode = "404", description = "Evento no encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 404", value = "{\"status\":404,\"message\":\"Recurso no encontrado\",\"timestamp\":\"2026-10-08T10:00:00\",\"errors\":[\"Recurso no encontrado\"]}"))),
        @ApiResponse(responseCode = "409", description = "Solo se pueden eliminar borradores",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 409", value = "{\"status\":409,\"message\":\"Conflicto\",\"timestamp\":\"2026-10-08T10:00:00\",\"errors\":[\"Conflicto con el estado actual\"]}")))
    })
    @DeleteMapping("/{eventoId}")
    public ResponseEntity<Void> eliminarBorrador(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId) {
        eventoAdminService.eliminarBorrador(eventoId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar eventos (admin)", description = "Lista todos los eventos incluyendo borradores y cancelados. Requiere rol ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado obtenido",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = EventoListadoResponse.class)))),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Se requiere rol ADMIN")
    })
    @GetMapping("/obtener")
    public ResponseEntity<List<EventoListadoResponse>> obtenerEventos() {
        return ResponseEntity.ok(eventoAdminService.obtenerEventos());
    }

    @Operation(summary = "Ver evento (admin)", description = "Devuelve el detalle de cualquier evento por ID. Requiere rol ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Detalle obtenido",
            content = @Content(schema = @Schema(implementation = EventoDetalleResponse.class))),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Se requiere rol ADMIN"),
        @ApiResponse(responseCode = "404", description = "Evento no encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 404", value = "{\"status\":404,\"message\":\"Recurso no encontrado\",\"timestamp\":\"2026-10-08T10:00:00\",\"errors\":[\"Recurso no encontrado\"]}")))
    })
    @GetMapping("/{eventoId}/obtener")
    public ResponseEntity<EventoDetalleResponse> obtenerEventoPorId(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId) {
        return ResponseEntity.ok(eventoAdminService.obtenerEventoPorId(eventoId));
    }
    
}
