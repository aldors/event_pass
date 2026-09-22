package com.aldo.event_pass.dto.tipo_boleto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TipoBoletoDisponibleResponse {
    
    private Long id;
    private String nombre;
    private BigDecimal precio;
    private Integer cantidadTotal;
    private Integer disponibles;
}