package com.aldo.event_pass.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.aldo.event_pass.enums.EstadoEvento;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "eventos")
@Getter
@Setter
@AllArgsConstructor 
@NoArgsConstructor 
@Builder
public class Evento {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private LocalDateTime fechaInicio;

    @Column(nullable = false)
    private LocalDateTime fechaFin;

    @Column(nullable = false, length = 255)
    private String ubicacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEvento estado;

    @Column(nullable = false)
    private Integer maxBoletosPorUsuario;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    @OneToMany(
        mappedBy = "evento",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<TipoBoleto> tiposBoleto = new ArrayList<>();

    
    @PrePersist
    public void prePersist() {

        LocalDateTime ahora = LocalDateTime.now();

        this.fechaCreacion = ahora;
        this.fechaActualizacion = ahora;

        if (this.estado == null) {
            this.estado = EstadoEvento.BORRADOR;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
