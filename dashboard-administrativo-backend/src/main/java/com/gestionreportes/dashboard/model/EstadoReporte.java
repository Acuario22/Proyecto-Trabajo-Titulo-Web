package com.gestionreportes.dashboard.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TBLESTADOREPORTE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadoReporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ESTADO")
    private Integer idEstado;

    @Column(name = "NOMBRE", length = 30, nullable = false, unique = true)
    private String nombre;

    @Column(name = "ORDEN", nullable = false, unique = true)
    private Integer orden;
}
