package com.aldo.event_pass.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aldo.event_pass.dto.pago.PagoResponse;
import com.aldo.event_pass.dto.reservacion.CompraDetalleResponse;
import com.aldo.event_pass.dto.reservacion.CompraResumenResponse;
import com.aldo.event_pass.dto.reservacion.ReservaResponse;
import com.aldo.event_pass.dto.reservacion.ReservarBoletosRequest;
import com.aldo.event_pass.exception.ApiError.ApiError;
import com.aldo.event_pass.service.interfaces.CompraService;

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

@Tag(name = "Compras", description = "Reserva, pago e historial de compras del usuario autenticado")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/compras")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CompraController {
    
    private final CompraService compraService;

    @Operation(summary = "Reservar boletos", description = "Crea una compra en estado pendiente y reserva stock. Requiere rol USER o ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reserva creada",
            content = @Content(schema = @Schema(implementation = ReservaResponse.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o stock insuficiente",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 400", value = "{\"status\":400,\"message\":\"Error de validación\",\"timestamp\":\"2026-10-08T10:00:00\",\"errors\":[\"La solicitud contiene datos inválidos\"]}"))),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "Evento o tipo de boleto no encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 404", value = "{\"status\":404,\"message\":\"Recurso no encontrado\",\"timestamp\":\"2026-10-08T10:00:00\",\"errors\":[\"Recurso no encontrado\"]}")))
    })
    @PostMapping("/reservar-boletos")
    public ResponseEntity<ReservaResponse> reservarBoletos(@Valid @RequestBody ReservarBoletosRequest reservarBoletosRequest){
        return ResponseEntity.ok(compraService.reservarBoletos(reservarBoletosRequest));
    }

    @Operation(summary = "Pagar compra", description = "Marca una compra pendiente como pagada y genera los boletos con QR. Requiere rol USER o ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pago registrado",
            content = @Content(schema = @Schema(implementation = PagoResponse.class))),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "La compra no pertenece al usuario"),
        @ApiResponse(responseCode = "404", description = "Compra no encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 404", value = "{\"status\":404,\"message\":\"Recurso no encontrado\",\"timestamp\":\"2026-10-08T10:00:00\",\"errors\":[\"Recurso no encontrado\"]}"))),
        @ApiResponse(responseCode = "409", description = "La compra ya fue pagada o expiró",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 409", value = "{\"status\":409,\"message\":\"Conflicto\",\"timestamp\":\"2026-10-08T10:00:00\",\"errors\":[\"Conflicto con el estado actual\"]}")))
    })
    @PostMapping("/{compraId}/pagar")
    public ResponseEntity<PagoResponse> pagar(
            @Parameter(description = "ID de la compra pendiente", example = "1") @PathVariable Long compraId){
        return ResponseEntity.ok(compraService.pagar(compraId));
    }

    @Operation(summary = "Listar mis compras", description = "Devuelve el historial resumido de compras del usuario autenticado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historial obtenido",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CompraResumenResponse.class)))),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @GetMapping("/mis-compras")
    public ResponseEntity<List<CompraResumenResponse>> obtenerCompras(){
        return ResponseEntity.ok(compraService.obtenerCompras());
    }

    @Operation(summary = "Ver detalle de compra", description = "Devuelve el detalle de una compra con sus boletos. Solo el dueño o un ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Detalle obtenido",
            content = @Content(schema = @Schema(implementation = CompraDetalleResponse.class))),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "La compra no pertenece al usuario"),
        @ApiResponse(responseCode = "404", description = "Compra no encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 404", value = "{\"status\":404,\"message\":\"Recurso no encontrado\",\"timestamp\":\"2026-10-08T10:00:00\",\"errors\":[\"Recurso no encontrado\"]}")))
    })
    @GetMapping("/{compraId}")
    public ResponseEntity<CompraDetalleResponse> obtenerCompraPorId(
            @Parameter(description = "ID de la compra", example = "1") @PathVariable Long compraId){
        return ResponseEntity.ok(compraService.obtenerCompraPorId(compraId));
    }
}
