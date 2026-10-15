package com.aldo.event_pass.dto.pago;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CheckoutResponse {
    
    private Long compraId;
    private String estado;
    private String checkoutUrl;
}
