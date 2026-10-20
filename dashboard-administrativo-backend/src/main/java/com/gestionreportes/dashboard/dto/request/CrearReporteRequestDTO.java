package com.gestionreportes.dashboard.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearReporteRequestDTO {

    @NotBlank(message = "El texto del reporte no puede estar vacío")
    private String textoReporte;

    private String direccion;

    @NotNull(message = "La categoría es obligatoria")
    private Integer idCategoria;

    @NotNull(message = "El sentimiento es obligatorio")
    private Integer idSentimiento;

    @NotNull(message = "La urgencia ML es obligatoria")
    private Integer idUrgenciaMl;

    @Builder.Default
    private String nivelRiesgo = "NINGUNO";

    @NotNull(message = "La urgencia final es obligatoria")
    private Integer idUrgenciaFinal;

    @Builder.Default
    private Boolean huboAjuste = false;
}
