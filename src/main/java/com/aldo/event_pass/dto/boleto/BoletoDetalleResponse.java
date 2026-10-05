package com.aldo.event_pass.dto.boleto;

import com.aldo.event_pass.enums.EstadoBoleto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BoletoDetalleResponse {

    private Long boletoId;
    private String tipoBoletoNombre;
    private String titularNombre;
    private String folio;
    private EstadoBoleto estado;
    /*
     * Indica si el PDF puede descargarse (compra pagada y evento vigente).
     * El frontend solo lee este flag, no replica las reglas.
     */
    private boolean descargable;
}
