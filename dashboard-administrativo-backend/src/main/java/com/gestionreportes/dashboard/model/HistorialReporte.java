package com.gestionreportes.dashboard.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TBLHISTORIAL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialReporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_HISTORIAL")
    private Integer idHistorial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_REPORTE", nullable = false)
    private Reporte reporte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ESTADO", nullable = false)
    private EstadoReporte estado;

    @Column(name = "FECHA_CAMBIO", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCambio = LocalDateTime.now();

    @Column(name = "OBSERVACION", length = 500)
    private String observacion;
}
