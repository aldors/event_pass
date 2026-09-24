package com.aldo.event_pass.service.interfaces;

import com.aldo.event_pass.dto.pago.PagoResponse;
import com.aldo.event_pass.dto.reservacion.ReservaResponse;
import com.aldo.event_pass.dto.reservacion.ReservarBoletosRequest;

public interface CompraService {
    
    public ReservaResponse reservarBoletos(ReservarBoletosRequest reservarBoletosRequest);
    // Se pasa como parametro el id de la compra 'reservada'
    public PagoResponse pagar(Long compraId);
}
