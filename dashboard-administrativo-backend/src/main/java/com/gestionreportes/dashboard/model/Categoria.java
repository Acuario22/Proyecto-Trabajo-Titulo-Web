package com.gestionreportes.dashboard.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TBLCATEGORIA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_CATEGORIA")
    private Integer idCategoria;

    @Column(name = "NOMBRE", length = 50, nullable = false, unique = true)
    private String nombre;

    @Column(name = "DESCRIPCION", length = 500)
    private String descripcion;
}
