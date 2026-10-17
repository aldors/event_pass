package com.aldo.event_pass.dto.pago;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Schema(description = "Sesión de checkout de Stripe para completar el pago")
public class CheckoutResponse {
    
    @Schema(description = "ID de la compra a pagar", example = "1")
    private Long compraId;
    @Schema(description = "Estado del pago pendiente", example = "PENDIENTE")
    private String estado;
    @Schema(description = "URL de la pasarela de Stripe para completar el pago", example = "https://checkout.stripe.com/c/pay/cs_test_abc123")
    private String checkoutUrl;
}
