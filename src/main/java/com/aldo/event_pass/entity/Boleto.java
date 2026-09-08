package com.aldo.event_pass.entity;

import java.util.UUID;

import com.aldo.event_pass.enums.EstadoBoleto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "boletos",
        indexes = {
                    @Index(name = "idx_boleto_estado", columnList = "estado")
            }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Boleto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detalle_compra_id", nullable = false)
    private DetalleCompra detalleCompra;

    @Column(nullable = false, unique = true, length = 36)
    private String folio;

    @Column(nullable = false, length = 200)
    private String titularNombre;

    @Column(nullable = false, unique = true, length = 36)
    private String codigoQr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoBoleto estado;

    
    @PrePersist
    public void prePersist() {

        if (folio == null) {
            folio = UUID.randomUUID().toString();
        }

        if (codigoQr == null) {
            codigoQr = UUID.randomUUID().toString();
        }

        if (estado == null) {
            estado = EstadoBoleto.ACTIVO;
        }
    }
}
