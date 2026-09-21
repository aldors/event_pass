package com.aldo.event_pass.service.interfaces;

import com.aldo.event_pass.dto.boleto.BoletoResponse;
import com.aldo.event_pass.dto.boleto.UsarBoletoResponse;

public interface BoletoService {

    public BoletoResponse verificarBoleto(String codigoQr);
    public UsarBoletoResponse usarBoleto(String codigoQr);
}
