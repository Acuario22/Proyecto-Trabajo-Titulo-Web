package com.gestionreportes.dashboard.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "TBLORDENTRABAJO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ORDEN")
    private Integer idOrden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_REPORTE", nullable = false)
    private Reporte reporte;

    @Column(name = "CUADRILLA", length = 100, nullable = false)
    private String cuadrilla;

    @Column(name = "RESPONSABLE", length = 100, nullable = false)
    private String responsable;

    @Column(name = "CANTIDAD_PERSONAL", nullable = false)
    private Short cantidadPersonal;

    @Column(name = "VEHICULO", length = 100)
    private String vehiculo;

    @Column(name = "FECHA_VISITA", nullable = false)
    private LocalDate fechaVisita;

    @Column(name = "HORA_VISITA", nullable = false)
    private LocalTime horaVisita;

    @Column(name = "DURACION_ESTIMADA", length = 50, nullable = false)
    private String duracionEstimada;

    @Column(name = "MATERIALES", length = 500)
    private String materiales;

    @Column(name = "INSTRUCCIONES", length = 1000, nullable = false)
    private String instrucciones;

    @Column(name = "FECHA_CIERRE_ADMIN")
    private LocalDateTime fechaCierreAdmin;

    @Column(name = "OBSERVACION_CIERRE_ADMIN", length = 500)
    private String observacionCierreAdmin;
}
