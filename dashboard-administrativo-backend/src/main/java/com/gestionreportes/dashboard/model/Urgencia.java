package com.gestionreportes.dashboard.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TBLURGENCIA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Urgencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_URGENCIA")
    private Integer idUrgencia;

    @Column(name = "NOMBRE", length = 20, nullable = false, unique = true)
    private String nombre;

    @Column(name = "NIVEL", nullable = false, unique = true)
    private Integer nivel;
}
