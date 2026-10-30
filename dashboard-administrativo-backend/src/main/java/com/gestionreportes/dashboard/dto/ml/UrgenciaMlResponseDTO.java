package com.gestionreportes.dashboard.dto.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO que mapea la respuesta del microservicio FastAPI con la predicción de Urgencia (SVM Lineal)
 * y la salida del motor de reglas de priorización (nivel de riesgo, urgencia final y flag de ajuste).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UrgenciaMlResponseDTO(
        @JsonProperty("urgencia_ml")
        String urgenciaMl,

        @JsonProperty("nivel_riesgo")
        String nivelRiesgo,

        @JsonProperty("urgencia_final")
        String urgenciaFinal,

        @JsonProperty("hubo_ajuste")
        Boolean huboAjuste,

        Double confianza,

        @JsonProperty("tiempo_ms")
        Double tiempoMs,

        @JsonProperty("urgencia_ml_original")
        String urgenciaMlOriginal,

        @JsonProperty("urgencia_final_original")
        String urgenciaFinalOriginal
) {}
