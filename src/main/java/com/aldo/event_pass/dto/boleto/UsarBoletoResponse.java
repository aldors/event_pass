package com.aldo.event_pass.dto.boleto;

import com.aldo.event_pass.enums.EstadoBoleto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UsarBoletoResponse {

    private String evento;
    private String titular;
    private String folio;
    private EstadoBoleto estado;
    private String mensaje;
}
