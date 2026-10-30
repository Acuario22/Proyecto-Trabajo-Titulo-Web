package com.gestionreportes.dashboard.dto.ml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para enviar el texto y la categoría detectada al microservicio de Machine Learning
 * para la clasificación de urgencia (SVM Lineal) y evaluación del motor de reglas de riesgo.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UrgenciaMlRequestDTO(
        @JsonProperty("texto")
        @NotBlank(message = "El texto para inferencia de urgencia no puede estar vacío")
        String texto,

        @JsonProperty("categoria")
        String categoria
) {
    public UrgenciaMlRequestDTO(String texto) {
        this(texto, null);
    }
}
