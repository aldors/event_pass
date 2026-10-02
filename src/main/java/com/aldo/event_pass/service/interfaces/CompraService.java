package com.aldo.event_pass.service.interfaces;

import java.util.List;

import com.aldo.event_pass.dto.pago.PagoResponse;
import com.aldo.event_pass.dto.reservacion.CompraDetalleResponse;
import com.aldo.event_pass.dto.reservacion.CompraResumenResponse;
import com.aldo.event_pass.dto.reservacion.ReservaResponse;
import com.aldo.event_pass.dto.reservacion.ReservarBoletosRequest;

public interface CompraService {
    
    public ReservaResponse reservarBoletos(ReservarBoletosRequest reservarBoletosRequest);
    // Se pasa como parametro el id de la compra 'reservada'
    public PagoResponse pagar(Long compraId);
    public List<CompraResumenResponse> obtenerCompras();
    public CompraDetalleResponse obtenerCompraPorId(Long compraId);
}
