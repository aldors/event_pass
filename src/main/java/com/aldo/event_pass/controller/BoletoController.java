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

@Tag(name = "Boletos", description = "Verificación pública, uso en acceso (ADMIN) y descarga de PDF")
@RestController
@RequestMapping("/boletos")
@RequiredArgsConstructor
public class BoletoController {

    private final BoletoService boletoService;
    
    @Operation(summary = "Verificar boleto", description = "Consulta pública del estado de un boleto por su código QR. Endpoint público.", security = {})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Boleto encontrado",
            content = @Content(schema = @Schema(implementation = BoletoResponse.class))),
        @ApiResponse(responseCode = "404", description = "Boleto no encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 404", value = "{\"status\":404,\"message\":\"Recurso no encontrado\",\"timestamp\":\"2026-10-08T10:00:00\",\"errors\":[\"Recurso no encontrado\"]}")))
    })
    @GetMapping("/verificar/{codigoQr}")
    public ResponseEntity<BoletoResponse>verificarBoleto(
            @Parameter(description = "Código QR del boleto", example = "EVT-AB12CD34") @PathVariable String codigoQr) {
        return ResponseEntity.ok(boletoService.verificarBoleto(codigoQr));
    }

    @Operation(summary = "Marcar boleto como usado", description = "Valida el ingreso en puerta escaneando el QR. Requiere rol ADMIN.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Boleto marcado como usado",
            content = @Content(schema = @Schema(implementation = UsarBoletoResponse.class))),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Se requiere rol ADMIN"),
        @ApiResponse(responseCode = "404", description = "Boleto no encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 404", value = "{\"status\":404,\"message\":\"Recurso no encontrado\",\"timestamp\":\"2026-10-08T10:00:00\",\"errors\":[\"Recurso no encontrado\"]}"))),
        @ApiResponse(responseCode = "409", description = "Boleto ya usado o no válido para ingreso",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 409", value = "{\"status\":409,\"message\":\"Conflicto\",\"timestamp\":\"2026-10-08T10:00:00\",\"errors\":[\"Conflicto con el estado actual\"]}")))
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{codigoQr}/usar")
    public ResponseEntity<UsarBoletoResponse> usarBoleto(
            @Parameter(description = "Código QR del boleto", example = "EVT-AB12CD34") @PathVariable String codigoQr) {
        return ResponseEntity.ok(boletoService.usarBoleto(codigoQr));
    }

    @Operation(summary = "Descargar boleto en PDF", description = "Genera el PDF del boleto con su QR. Solo el dueño o un ADMIN. Requiere JWT.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "PDF generado",
            content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Sin permiso sobre el boleto"),
        @ApiResponse(responseCode = "404", description = "Boleto no encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class),
                examples = @ExampleObject(name = "Error 404", value = "{\"status\":404,\"message\":\"Recurso no encontrado\",\"timestamp\":\"2026-10-08T10:00:00\",\"errors\":[\"Recurso no encontrado\"]}")))
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
