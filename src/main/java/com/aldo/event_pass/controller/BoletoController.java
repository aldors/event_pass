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
import com.aldo.event_pass.service.interfaces.BoletoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/boletos")
@RequiredArgsConstructor
public class BoletoController {

    private final BoletoService boletoService;
    
    @GetMapping("/verificar/{codigoQr}")
    public ResponseEntity<BoletoResponse>erificarBoleto(@PathVariable String codigoQr) {
        return ResponseEntity.ok(boletoService.verificarBoleto(codigoQr));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{codigoQr}/usar")
    public ResponseEntity<UsarBoletoResponse> usarBoleto(@PathVariable String codigoQr) {
        return ResponseEntity.ok(boletoService.usarBoleto(codigoQr));
    }

    @PreAuthorize("hasRole('USER', 'ADMIN')")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> generarPdf(@PathVariable Long id) {

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
