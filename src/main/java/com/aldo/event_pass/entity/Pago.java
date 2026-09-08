package com.aldo.event_pass.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.aldo.event_pass.enums.EstadoPago;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "pagos",
        uniqueConstraints = { 
            @UniqueConstraint( 
                name = "uk_proveedor_id_pago", 
                columnNames = {"proveedor", "id_pago_externo"} 
            ) 
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Pago {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @Column(nullable = false, length = 30)
    private String proveedor;

    @Column(name = "id_pago_externo", nullable = false, length = 150)
    private String idPagoExterno;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaConfirmacion;


    @PrePersist 
    public void prePersist() {
        
        if (estado == null) {
            estado = EstadoPago.PENDIENTE; 
        }

        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now(); 
        } 
    }
}
