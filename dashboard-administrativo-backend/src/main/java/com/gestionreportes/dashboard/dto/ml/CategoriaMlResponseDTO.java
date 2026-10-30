package com.gestionreportes.dashboard.dto.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO que mapea la respuesta del microservicio FastAPI con la predicción del modelo de Categoría.
 * Incluye @JsonIgnoreProperties para tolerar campos adicionales como categoria_original.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CategoriaMlResponseDTO(
        String categoria,
        Double confianza,
        @JsonProperty("tiempo_ms")
        Double tiempo_ms,
        @JsonProperty("categoria_original")
        String categoria_original
) {}
