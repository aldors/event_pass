package com.aldo.event_pass.mapper;

import org.springframework.stereotype.Component;

import com.aldo.event_pass.dto.tipo_boleto.TipoBoletoDisponibleResponse;
import com.aldo.event_pass.dto.tipo_boleto.TipoBoletoRequest;
import com.aldo.event_pass.dto.tipo_boleto.TipoBoletoResponse;
import com.aldo.event_pass.entity.TipoBoleto;

@Component
public class TipoBoletoMapper {
    
    public static TipoBoleto toEntity(TipoBoletoRequest tipoBoletoRequest){

        return TipoBoleto.builder()
            .nombre(tipoBoletoRequest.getNombre())
            .precio(tipoBoletoRequest.getPrecio())
            .cantidadTotal(tipoBoletoRequest.getCantidadTotal())
            .build();
    }

    public static TipoBoletoResponse toResponse(TipoBoleto tipoBoleto){

        return new TipoBoletoResponse(
            tipoBoleto.getId(),
            tipoBoleto.getNombre(),
            tipoBoleto.getPrecio(),
            tipoBoleto.getCantidadTotal()
        );
    }

    public static TipoBoletoDisponibleResponse toDisponibleResponse(TipoBoleto tipoBoleto){
        return new TipoBoletoDisponibleResponse(
            tipoBoleto.getId(),
            tipoBoleto.getNombre(),
            tipoBoleto.getPrecio(),
            tipoBoleto.getCantidadTotal()
        );
    }
}
