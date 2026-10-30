package com.gestionreportes.dashboard.service;

import com.gestionreportes.dashboard.dto.ml.UrgenciaMlResponseDTO;

/**
 * Servicio de inferencia para interactuar con los modelos de Machine Learning.
 */
public interface MlInferenceService {

    /**
     * Predice la categoría de infraestructura urbana a partir del texto del reporte.
     * Retorna el nombre normalizado (ej: PAVIMENTACION, ALCANTARILLADO, ILUMINACION).
     *
     * @param texto Descripción ciudadana del problema reportado
     * @return Nombre de la categoría normalizada
     */
    String predecirCategoria(String texto);

    /**
     * Predice el sentimiento ciudadano expresado en el reporte.
     * Retorna el nombre normalizado (ej: ENOJADO, NEUTRAL, FRUSTRADO).
     *
     * @param texto Descripción ciudadana del problema reportado
     * @return Nombre del sentimiento normalizado
     */
    String predecirSentimiento(String texto);

    /**
     * Predice la urgencia mediante SVM Lineal y evalúa el motor de priorización basado en reglas de riesgo.
     * Retorna un DTO con urgenciaMl, nivelRiesgo, urgenciaFinal y huboAjuste.
     *
     * @param texto Descripción ciudadana del problema reportado
     * @param categoria Categoría clasificada previamente (opcional para motor de reglas)
     * @return DTO con la evaluación completa de urgencia y riesgo
     */
    UrgenciaMlResponseDTO predecirUrgencia(String texto, String categoria);

    /**
     * Sobrecarga de predecirUrgencia sin especificar categoría.
     *
     * @param texto Descripción ciudadana del problema reportado
     * @return DTO con la evaluación completa de urgencia y riesgo
     */
    default UrgenciaMlResponseDTO predecirUrgencia(String texto) {
        return predecirUrgencia(texto, null);
    }
}
