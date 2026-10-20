package com.gestionreportes.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrazabilidadReporteDTO {
    private String codigo;
    private String titulo;
    private String descripcion;
    private String direccion;
    private String fechaIngreso;
    private String urgencia;
    private String sentimiento;
    private String estadoActual;
    private String detalleEstadoActual;
    private List<PasoLineaTiempoDTO> pasos;
}
