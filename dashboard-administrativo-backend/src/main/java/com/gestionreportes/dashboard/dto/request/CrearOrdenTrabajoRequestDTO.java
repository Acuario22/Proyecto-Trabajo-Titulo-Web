package com.gestionreportes.dashboard.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos para crear o actualizar una orden de trabajo")
public class CrearOrdenTrabajoRequestDTO {

    @NotBlank(message = "La cuadrilla es obligatoria")
    @Size(max = 100, message = "La cuadrilla no puede exceder 100 caracteres")
    @Schema(description = "Nombre de la cuadrilla especializada", example = "Cuadrilla de Obras Viales (Pavimentación)")
    private String cuadrilla;

    @NotBlank(message = "El responsable es obligatorio")
    @Size(max = 100, message = "El responsable no puede exceder 100 caracteres")
    @Schema(description = "Nombre completo del responsable de cuadrilla", example = "Carlos Mendoza")
    private String responsable;

    @NotNull(message = "La cantidad de personal es obligatoria")
    @Min(value = 1, message = "Debe haber al menos 1 persona asignada")
    @Schema(description = "Cantidad de operarios asignados", example = "4")
    private Integer cantidadPersonal;

    @Size(max = 100, message = "El vehículo no puede exceder 100 caracteres")
    @Schema(description = "Vehículo o unidad asignada", example = "Camión Tolva OP-14")
    private String vehiculo;

    @NotNull(message = "La fecha de visita es obligatoria")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Fecha programada de visita (formato yyyy-MM-dd)", example = "2026-10-25")
    private LocalDate fechaVisita;

    @NotNull(message = "La hora de visita es obligatoria")
    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "Hora programada de visita (formato HH:mm)", example = "09:30")
    private LocalTime horaVisita;

    @NotBlank(message = "La duración estimada es obligatoria")
    @Size(max = 50, message = "La duración estimada no puede exceder 50 caracteres")
    @Schema(description = "Duración estimada del trabajo", example = "Media jornada (4 horas)")
    private String duracionEstimada;

    @Size(max = 500, message = "Los materiales no pueden exceder 500 caracteres")
    @Schema(description = "Materiales o equipos requeridos", example = "Mezcla asfáltica en frío, compactadora manual")
    private String materiales;

    @NotBlank(message = "Las instrucciones son obligatorias")
    @Size(max = 1000, message = "Las instrucciones no pueden exceder 1000 caracteres")
    @Schema(description = "Instrucciones de trabajo y medidas operativas", example = "Reparación de bache profundo y señalización perimetral.")
    private String instrucciones;
}
