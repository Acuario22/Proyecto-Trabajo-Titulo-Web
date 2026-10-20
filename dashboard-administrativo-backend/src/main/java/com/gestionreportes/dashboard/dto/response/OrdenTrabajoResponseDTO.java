package com.gestionreportes.dashboard.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Detalle de orden de trabajo asociada a un reporte")
public class OrdenTrabajoResponseDTO {

    @Schema(description = "ID de la orden de trabajo", example = "1")
    private Integer idOrden;

    @Schema(description = "ID del reporte asociado", example = "1")
    private Integer idReporte;

    @Schema(description = "Código de negocio del reporte", example = "RPT-2024-0001")
    private String codigoReporte;

    @Schema(description = "Nombre de la cuadrilla asignada", example = "Cuadrilla de Obras Viales (Pavimentación)")
    private String cuadrilla;

    @Schema(description = "Responsable de la cuadrilla", example = "Carlos Mendoza")
    private String responsable;

    @Schema(description = "Cantidad de personal asignado", example = "4")
    private Integer cantidadPersonal;

    @Schema(description = "Vehículo o unidad asignada", example = "Camión Tolva OP-14")
    private String vehiculo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Fecha de la visita (yyyy-MM-dd)", example = "2026-10-25")
    private LocalDate fechaVisita;

    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "Hora de la visita (HH:mm)", example = "09:30")
    private LocalTime horaVisita;

    @Schema(description = "Duración estimada", example = "Media jornada (4 horas)")
    private String duracionEstimada;

    @Schema(description = "Materiales requeridos", example = "Mezcla asfáltica, compactadora")
    private String materiales;

    @Schema(description = "Instrucciones para la cuadrilla", example = "Bacheo y compactación completa.")
    private String instrucciones;

    @Schema(description = "Estado actual del reporte tras la asignación", example = "Iniciado")
    private String estadoReporte;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Fecha y hora del cierre administrativo", example = "2026-10-26 16:30:00")
    private LocalDateTime fechaCierreAdmin;

    @Schema(description = "Observación o dictamen de cierre registrado por el administrador", example = "Se inspeccionó el trabajo de bacheo. Obra conforme.")
    private String observacionCierreAdmin;

    @Schema(description = "Informe o bitácora técnica ingresada por la cuadrilla al completar el trabajo", example = "Trabajo ejecutado en terreno por Cuadrilla de Obras Viales. Se cumplieron las instrucciones operativas.")
    private String comentarioCuadrilla;
}
