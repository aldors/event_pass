package com.aldo.event_pass.dto.projection;

import com.aldo.event_pass.enums.EstadoBoleto;

public interface BoletoDetalleProjection {

    Long getBoletoId();

    String getTipoBoletoNombre();

    String getTitularNombre();

    String getFolio();

    EstadoBoleto getEstado();
}
