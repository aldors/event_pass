package com.aldo.event_pass.service.interfaces;

import com.aldo.event_pass.dto.pago.PagoResponse;
import com.aldo.event_pass.dto.reservacion.ReservaResponse;
import com.aldo.event_pass.dto.reservacion.ReservarBoletosRequest;

public interface CompraService {
    
    public ReservaResponse reservarBoletos(ReservarBoletosRequest reservarBoletosRequest);
    public PagoResponse pagar(Long compraRId);
}
