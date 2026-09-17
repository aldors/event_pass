package com.aldo.event_pass.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aldo.event_pass.dto.reservacion.ReservaResponse;
import com.aldo.event_pass.dto.reservacion.ReservarBoletosRequest;
import com.aldo.event_pass.service.interfaces.CompraService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/compras")
@RequiredArgsConstructor
public class CompraController {
    
    private final CompraService compraService;

    @PostMapping("/reservar-boletos")
    public ResponseEntity<ReservaResponse> reservarBoletos(@Valid @RequestBody ReservarBoletosRequest reservarBoletosRequest){
        return ResponseEntity.ok(compraService.reservarBoletos(reservarBoletosRequest));
    }
}
