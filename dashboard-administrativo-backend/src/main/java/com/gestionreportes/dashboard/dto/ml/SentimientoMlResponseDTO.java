package com.gestionreportes.dashboard.dto.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO que mapea la respuesta del microservicio FastAPI con la predicción del modelo de Sentimiento.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SentimientoMlResponseDTO(
        String sentimiento,
        Double confianza,
        @JsonProperty("tiempo_ms")
        Double tiempo_ms,
        @JsonProperty("sentimiento_original")
        String sentimiento_original
) {}
