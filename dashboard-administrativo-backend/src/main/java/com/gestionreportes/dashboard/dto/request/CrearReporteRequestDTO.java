package com.gestionreportes.dashboard.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la creación de un nuevo reporte ciudadano.
 * El ciudadano solo necesita enviar 'textoReporte' y 'direccion'.
 * La categoría es clasificada automáticamente por el microservicio de Machine
 * Learning,
 * y los campos restantes (sentimiento, urgencia, riesgo) son gestionados por el
 * backend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para la creación de reportes ciudadanos")
public class CrearReporteRequestDTO {

    @NotBlank(message = "El texto del reporte no puede estar vacío")
    @Schema(description = "Descripción del problema reportado por el ciudadano", example = "Se observa rotura de matriz de agua con fuga constante en plena vereda.")
    private String textoReporte;

    @Schema(description = "Dirección o ubicación geográfica del incidente (opcional; si se omite o viene vacía se asigna 'No especificada')", example = "Av. Libertad con Calle 5", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    private String direccion;

    // Campos secundarios opcionales (ocultos en la plantilla de Swagger para que el
    // usuario solo envíe texto y dirección)
    @Schema(hidden = true)
    private Integer idCategoria;

    @Schema(hidden = true)
    private Integer idSentimiento;

    @Schema(hidden = true)
    private Integer idUrgenciaMl;

    @Schema(hidden = true)
    private String nivelRiesgo;

    @Schema(hidden = true)
    private Integer idUrgenciaFinal;

    @Schema(hidden = true)
    private Boolean huboAjuste;
}
