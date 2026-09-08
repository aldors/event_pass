package com.aldo.event_pass.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "tipos_boleto",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_evento_nombre",
                        columnNames = {"evento_id", "nombre"}
                )
        })
@Getter 
@Setter
@AllArgsConstructor
@NoArgsConstructor 
@Builder
public class TipoBoleto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private Integer cantidadTotal;

    @OneToMany(mappedBy = "tipoBoleto")
    private List<DetalleCompra> detallesCompra = new ArrayList<>();
}
