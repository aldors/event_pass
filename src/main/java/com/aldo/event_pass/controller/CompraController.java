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

import com.aldo.event_pass.dto.pago.CheckoutResponse;
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

    @Operation(summary = "Reservar boletos", description = "Crea una compra RESERVADA con TTL de 15 minutos y reserva stock; solo se permite si el estado efectivo del evento es PUBLICADO (no INICIADO). Requiere rol USER o ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reserva creada",
            content = @Content(schema = @Schema(implementation = ReservaResponse.class))),
        @ApiResponse(responseCode = "400", description = "Error de validación de campos",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 400", value = "{\"status\":400,\"message\":\"Error de validación\",\"timestamp\":\"2026-12-20T19:00:00\",\"errors\":[\"Debe seleccionar al menos un boleto\"]}"))),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "Evento o tipo de boleto no encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 404", value = "{\"status\":404,\"message\":\"Tipo de boleto inexistente\",\"timestamp\":\"2026-12-20T19:00:00\",\"errors\":[\"El tipo de boleto no existe\"]}"))),
        @ApiResponse(responseCode = "409", description = "Stock insuficiente, límite excedido o estado efectivo del evento distinto de PUBLICADO",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 409", value = "{\"status\":409,\"message\":\"No hay suficientes boletos\",\"timestamp\":\"2026-12-20T19:00:00\",\"errors\":[\"No hay suficientes boletos disponibles para: General\"]}")))
    })
    @PostMapping("/reservar-boletos")
    public ResponseEntity<ReservaResponse> reservarBoletos(@Valid @RequestBody ReservarBoletosRequest reservarBoletosRequest){
        return ResponseEntity.ok(compraService.reservarBoletos(reservarBoletosRequest));
    }

    @Operation(summary = "Pagar compra", description = "Crea una sesión de checkout en Stripe (expira en 30 min) y devuelve la URL de pago; al crearla extiende la reserva de la compra a 30 minutos. Si ya existe una sesión vigente, la reutiliza sin extenderla. Requiere rol USER o ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sesión de checkout creada",
            content = @Content(schema = @Schema(implementation = CheckoutResponse.class))),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "Compra no encontrada para el usuario autenticado",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 404", value = "{\"status\":404,\"message\":\"Compra no encontrada\",\"timestamp\":\"2026-12-20T19:00:00\",\"errors\":[\"Compra no encontrada\"]}"))),
        @ApiResponse(responseCode = "409", description = "Compra ya pagada, no reservada, reserva expirada, sesión abierta o datos de pago inconsistentes",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 409", value = "{\"status\":409,\"message\":\"Reserva expirada\",\"timestamp\":\"2026-12-20T19:00:00\",\"errors\":[\"La reserva expiró\"]}"))),
        @ApiResponse(responseCode = "502", description = "Stripe no devolvió URL de checkout, devolvió una sesión inválida o no está disponible",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 502", value = "{\"status\":502,\"message\":\"No fue posible iniciar el pago con Stripe\",\"timestamp\":\"2026-12-20T19:00:00\",\"errors\":[\"No fue posible iniciar el pago con Stripe\"]}")))
    })
    @PostMapping("/{compraId}/pagar")
    public ResponseEntity<CheckoutResponse> pagar(
            @Parameter(description = "ID de la compra reservada", example = "1") @PathVariable Long compraId){
        return ResponseEntity.ok(compraService.pagar(compraId));
    }

    @Operation(summary = "Verificar pago", description = "Reconcilia contra Stripe el pago pendiente y devuelve el detalle actualizado. Es un respaldo del webhook e idempotente: puede devolver la compra aún RESERVADA si la sesión sigue abierta. Requiere rol USER o ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Detalle de la compra",
            content = @Content(schema = @Schema(implementation = CompraDetalleResponse.class))),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "Compra o pago asociado a la sesión no encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 404", value = "{\"status\":404,\"message\":\"Compra no encontrada\",\"timestamp\":\"2026-12-20T19:00:00\",\"errors\":[\"Compra no encontrada\"]}"))),
        @ApiResponse(responseCode = "409", description = "La sesión sigue abierta, no coincide con la compra o el importe/moneda no coinciden",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 409", value = "{\"status\":409,\"message\":\"La sesión de pago todavía está abierta\",\"timestamp\":\"2026-12-20T19:00:00\",\"errors\":[\"El pago aún no se ha completado\"]}"))),
        @ApiResponse(responseCode = "502", description = "Stripe no está disponible, devolvió una sesión inválida o el evento no pudo interpretarse",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 502", value = "{\"status\":502,\"message\":\"El servicio de pagos no está disponible\",\"timestamp\":\"2026-12-20T19:00:00\",\"errors\":[\"No fue posible verificar el pago con Stripe\"]}")))
    })
    @PostMapping("/{compraId}/verificar-pago")
    public ResponseEntity<CompraDetalleResponse> verificarPago(
            @Parameter(description = "ID de la compra a verificar", example = "1") @PathVariable Long compraId){
        return ResponseEntity.ok(compraService.verificarPago(compraId));
    }

    @Operation(summary = "Listar mis compras", description = "Devuelve el historial resumido de compras del usuario autenticado. Requiere rol USER o ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historial obtenido",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CompraResumenResponse.class)))),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @GetMapping("/mis-compras")
    public ResponseEntity<List<CompraResumenResponse>> obtenerCompras(){
        return ResponseEntity.ok(compraService.obtenerCompras());
    }

    @Operation(summary = "Ver detalle de compra", description = "Devuelve el detalle de una compra con sus boletos y su flag descargable. Solo el dueño o un ADMIN; STAFF no tiene acceso.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Detalle obtenido",
            content = @Content(schema = @Schema(implementation = CompraDetalleResponse.class))),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "La compra no pertenece al usuario"),
        @ApiResponse(responseCode = "404", description = "Compra no encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 404", value = "{\"status\":404,\"message\":\"Compra no encontrada\",\"timestamp\":\"2026-12-20T19:00:00\",\"errors\":[\"Compra no encontrada\"]}")))
    })
    @GetMapping("/{compraId}")
    public ResponseEntity<CompraDetalleResponse> obtenerCompraPorId(
            @Parameter(description = "ID de la compra", example = "1") @PathVariable Long compraId){
        return ResponseEntity.ok(compraService.obtenerCompraPorId(compraId));
    }
}
