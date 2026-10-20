package com.gestionreportes.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasoLineaTiempoDTO {
    private Integer numero;
    private String titulo;
    private String fecha;
    private String estado;      // "completado", "en-proceso", "pendiente"
    private String descripcion;
}
