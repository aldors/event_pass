package com.aldo.event_pass.service.interfaces;

import java.util.List;

import com.aldo.event_pass.dto.pago.CheckoutResponse;
import com.aldo.event_pass.dto.reservacion.CompraDetalleResponse;
import com.aldo.event_pass.dto.reservacion.CompraResumenResponse;
import com.aldo.event_pass.dto.reservacion.ReservaResponse;
import com.aldo.event_pass.dto.reservacion.ReservarBoletosRequest;

public interface CompraService {
    
    public ReservaResponse reservarBoletos(ReservarBoletosRequest reservarBoletosRequest);
    // Se pasa como parametro el id de la compra 'reservada'
    public CheckoutResponse pagar(Long compraId);
    // Reconcilia el pago pendiente contra Stripe y devuelve el detalle actualizado
    public CompraDetalleResponse verificarPago(Long compraId);
    public List<CompraResumenResponse> obtenerCompras();
    public CompraDetalleResponse obtenerCompraPorId(Long compraId);
}
