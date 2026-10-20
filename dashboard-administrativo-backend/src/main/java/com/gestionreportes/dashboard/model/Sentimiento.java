package com.gestionreportes.dashboard.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TBLSENTIMIENTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sentimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_SENTIMIENTO")
    private Integer idSentimiento;

    @Column(name = "NOMBRE", length = 20, nullable = false, unique = true)
    private String nombre;
}
