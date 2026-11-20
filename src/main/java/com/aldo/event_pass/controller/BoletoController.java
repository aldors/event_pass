package com.aldo.event_pass.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aldo.event_pass.dto.boleto.BoletoResponse;
import com.aldo.event_pass.dto.boleto.UsarBoletoResponse;
import com.aldo.event_pass.exception.ApiError.ApiError;
import com.aldo.event_pass.service.interfaces.BoletoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Boletos", description = "Verificación pública, uso en acceso (ADMIN o STAFF) y descarga de PDF")
@RestController
@RequestMapping("/boletos")
@RequiredArgsConstructor
public class BoletoController {

    private final BoletoService boletoService;
    
    @Operation(summary = "Verificar boleto", description = "Consulta pública del estado de un boleto por su código QR. El campo valido es true solo si el boleto está ACTIVO, la compra está PAGADA y el estado efectivo del evento es PUBLICADO o INICIADO; un boleto inválido no genera error. Endpoint público.", security = {})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Boleto encontrado (revisar el campo valido)",
            content = @Content(schema = @Schema(implementation = BoletoResponse.class))),
        @ApiResponse(responseCode = "404", description = "Boleto no encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 404", value = "{\"status\":404,\"message\":\"Boleto no encontrado\",\"timestamp\":\"2026-12-20T19:00:00\",\"errors\":[\"No existe el boleto solicitado\"]}")))
    })
    @GetMapping("/verificar/{codigoQr}")
    public ResponseEntity<BoletoResponse>verificarBoleto(
            @Parameter(description = "Código QR impreso en el boleto (UUID distinto del folio)", example = "9f8e7d6c-5b4a-4321-9876-1234567890ab") @PathVariable String codigoQr) {
        return ResponseEntity.ok(boletoService.verificarBoleto(codigoQr));
    }

    @Operation(summary = "Marcar boleto como usado", description = "Valida el ingreso en puerta escaneando el QR. Exige compra PAGADA y estado efectivo del evento PUBLICADO o INICIADO; los eventos INICIADOS sí permiten acceso, los CANCELADOS/FINALIZADOS no. Requiere rol ADMIN o STAFF.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Boleto marcado como usado",
            content = @Content(schema = @Schema(implementation = UsarBoletoResponse.class))),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Se requiere rol ADMIN o STAFF"),
        @ApiResponse(responseCode = "402", description = "La compra del boleto no está PAGADA",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 402", value = "{\"status\":402,\"message\":\"Boleto no pagado\",\"timestamp\":\"2026-12-20T19:00:00\",\"errors\":[\"El boleto no está pagado\"]}"))),
        @ApiResponse(responseCode = "404", description = "Boleto no encontrado o evento en BORRADOR",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 404", value = "{\"status\":404,\"message\":\"Boleto no encontrado\",\"timestamp\":\"2026-12-20T19:00:00\",\"errors\":[\"No existe el boleto solicitado\"]}"))),
        @ApiResponse(responseCode = "409", description = "Boleto ya utilizado/invalidado o evento finalizado",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 409", value = "{\"status\":409,\"message\":\"Boleto utilizado\",\"timestamp\":\"2026-12-20T19:00:00\",\"errors\":[\"Este boleto ya fue utilizado\"]}"))),
        @ApiResponse(responseCode = "410", description = "El evento fue cancelado",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 410", value = "{\"status\":410,\"message\":\"Evento cancelado\",\"timestamp\":\"2026-12-20T19:00:00\",\"errors\":[\"El evento ha sido cancelado\"]}")))
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PostMapping("/{codigoQr}/usar")
    public ResponseEntity<UsarBoletoResponse> usarBoleto(
            @Parameter(description = "Código QR impreso en el boleto", example = "9f8e7d6c-5b4a-4321-9876-1234567890ab") @PathVariable String codigoQr) {
        return ResponseEntity.ok(boletoService.usarBoleto(codigoQr));
    }

    @Operation(summary = "Descargar boleto en PDF", description = "Genera el PDF del boleto con su QR. Solo el dueño o un ADMIN (STAFF no tiene acceso); exige compra PAGADA y que el estado efectivo del evento no sea BORRADOR, CANCELADO ni FINALIZADO (INICIADO permitido). Requiere JWT.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "PDF generado",
            content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Sin permiso sobre el boleto"),
        @ApiResponse(responseCode = "402", description = "La compra del boleto no está PAGADA",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 402", value = "{\"status\":402,\"message\":\"Boleto no pagado\",\"timestamp\":\"2026-12-20T19:00:00\",\"errors\":[\"El boleto no está pagado\"]}"))),
        @ApiResponse(responseCode = "404", description = "Boleto no encontrado o evento en BORRADOR",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 404", value = "{\"status\":404,\"message\":\"Boleto no encontrado\",\"timestamp\":\"2026-12-20T19:00:00\",\"errors\":[\"No existe el boleto solicitado\"]}"))),
        @ApiResponse(responseCode = "409", description = "El evento finalizó",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 409", value = "{\"status\":409,\"message\":\"Evento finalizado\",\"timestamp\":\"2026-12-20T19:00:00\",\"errors\":[\"El evento ya finalizó\"]}"))),
        @ApiResponse(responseCode = "410", description = "El evento fue cancelado",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 410", value = "{\"status\":410,\"message\":\"Evento cancelado\",\"timestamp\":\"2026-12-20T19:00:00\",\"errors\":[\"El evento ha sido cancelado\"]}"))),
        @ApiResponse(responseCode = "500", description = "No fue posible generar el boleto pdf",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 500", value = "{\"status\":500,\"message\":\"No fue posible generar el boleto pdf\",\"timestamp\":\"2026-12-20T19:00:00\",\"errors\":[\"No fue posible generar el boleto pdf\"]}")))
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> generarPdf(
            @Parameter(description = "ID del boleto", example = "1") @PathVariable Long id) {

        byte[] pdf = boletoService.generarPdf(id);

        return ResponseEntity.ok().header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=boleto-" + id + ".pdf"
                )
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .body(pdf);
    }
}
