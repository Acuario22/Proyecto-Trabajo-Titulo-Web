package com.gestionreportes.dashboard.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TBLREPORTES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_REPORTE")
    private Integer idReporte;

    @Column(name = "FECHA_INGRESO", nullable = false)
    @Builder.Default
    private LocalDateTime fechaIngreso = LocalDateTime.now();

    @Column(name = "TEXTO_REPORTE", nullable = false, columnDefinition = "TEXT")
    private String textoReporte;

    @Column(name = "DIRECCION", length = 255)
    private String direccion;

    // Salida de los modelos de Machine Learning
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CATEGORIA", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_SENTIMIENTO", nullable = false)
    private Sentimiento sentimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_URGENCIA_ML", nullable = false)
    private Urgencia urgenciaMl;

    // Salida del motor de priorización basado en reglas
    @Enumerated(EnumType.STRING)
    @Column(name = "NIVEL_RIESGO", nullable = false, length = 10)
    @Builder.Default
    private NivelRiesgo nivelRiesgo = NivelRiesgo.NINGUNO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_URGENCIA_FINAL", nullable = false)
    private Urgencia urgenciaFinal;

    @Column(name = "HUBO_AJUSTE", nullable = false)
    @Builder.Default
    private Boolean huboAjuste = false;

    // Gestión: estado actual del reporte
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ESTADO", nullable = false)
    private EstadoReporte estado;
}
