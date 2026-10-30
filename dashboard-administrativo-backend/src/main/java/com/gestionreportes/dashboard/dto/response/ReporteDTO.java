package com.gestionreportes.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Resumen de un reporte ciudadano para la tabla del panel administrativo")
public class ReporteDTO {

    @Schema(description = "Clave primaria numérica del reporte (PK de TBLREPORTES)", example = "1")
    private Integer id;

    @Schema(description = "Código de visualización con formato de negocio", example = "RPT-2026-0001")
    private String codigo;

    @Schema(description = "Nombre de la categoría del reporte", example = "Pavimentacion")
    private String categoria;

    @Schema(description = "Texto descriptivo del reporte ciudadano")
    private String texto;

    @Schema(description = "Dirección donde se reportó el problema", example = "Av. Libertad 1240, Sector Centro")
    private String direccion;

    @Schema(description = "Fecha de ingreso del reporte (formato dd/MM/yyyy)", example = "15/10/2026")
    private String fechaIngreso;

    @Schema(description = "Nivel de urgencia final aplicado al reporte", example = "Alta")
    private String urgencia;

    @Schema(description = "Predicción original de urgencia del modelo Machine Learning (ID_URGENCIA_ML)", example = "Media")
    private String urgenciaMl;

    @Schema(description = "Sentimiento detectado en el texto del reporte", example = "Enojado")
    private String sentimiento;

    @Schema(description = "Estado actual del reporte en el flujo de gestión", example = "Iniciado")
    private String estado;

    @Schema(description = "ID numérico del estado del reporte (1 = No iniciado, 2 = Iniciado, 3 = En revisión, 4 = Finalizado)", example = "2")
    private Integer idEstado;

    @Schema(description = "Nivel de riesgo calculado por el motor de priorización", example = "ALTO", allowableValues = {
            "NINGUNO", "BAJO", "MEDIO", "ALTO", "CRITICO" })
    private String nivelRiesgo;

    @Schema(description = "Indica si hubo ajuste manual de urgencia respecto a la predicción ML")
    private Boolean huboAjuste;
}
