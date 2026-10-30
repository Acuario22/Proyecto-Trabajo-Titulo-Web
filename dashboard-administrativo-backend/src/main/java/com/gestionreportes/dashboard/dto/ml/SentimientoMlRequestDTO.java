package com.gestionreportes.dashboard.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para enviar el texto del reporte ciudadano al microservicio de Machine Learning
 * para la inferencia de sentimiento.
 */
public record SentimientoMlRequestDTO(
        @JsonProperty("texto")
        @NotBlank(message = "El texto para inferencia no puede estar vacío")
        String texto
) {}
