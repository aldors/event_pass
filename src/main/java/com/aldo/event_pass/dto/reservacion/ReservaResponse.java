package com.aldo.event_pass.dto.reservacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReservaResponse {

    private Long compraId;
    private BigDecimal total;
    private LocalDateTime expiraEn;
}
