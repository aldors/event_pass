package com.aldo.event_pass.controller;

import org.springframework.http.ResponseEntity;
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

    @PostMapping("/{codigoQr}/usar")
    public ResponseEntity<UsarBoletoResponse> usarBoleto(@PathVariable String codigoQr) {
        return ResponseEntity.ok(boletoService.usarBoleto(codigoQr));
    }

}
