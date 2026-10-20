package com.gestionreportes.dashboard.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud para registrar la conformidad administrativa y finalizar un reporte")
public class CerrarReporteRequestDTO {

    @NotBlank(message = "La observación de cierre administrativo es obligatoria")
    @Size(max = 500, message = "La observación no puede exceder los 500 caracteres")
    @Schema(description = "Observación o dictamen de cierre registrado por el administrador", 
            example = "Se inspeccionó el trabajo de bacheo ejecutado por la cuadrilla. Obra conforme y calzada habilitada.")
    private String observacionCierreAdmin;
}
