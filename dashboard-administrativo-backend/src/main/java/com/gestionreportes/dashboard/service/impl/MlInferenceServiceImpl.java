package com.gestionreportes.dashboard.service.impl;

import com.gestionreportes.dashboard.dto.ml.CategoriaMlRequestDTO;
import com.gestionreportes.dashboard.dto.ml.CategoriaMlResponseDTO;
import com.gestionreportes.dashboard.dto.ml.SentimientoMlRequestDTO;
import com.gestionreportes.dashboard.dto.ml.SentimientoMlResponseDTO;
import com.gestionreportes.dashboard.dto.ml.UrgenciaMlRequestDTO;
import com.gestionreportes.dashboard.dto.ml.UrgenciaMlResponseDTO;
import com.gestionreportes.dashboard.service.MlInferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.text.Normalizer;

/**
 * Implementación del cliente de inferencia ML utilizando RestClient hacia FastAPI.
 * Soporta clasificación de Categoría (Fase 1), Sentimiento (Fase 2) y Urgencia + Reglas de Riesgo (Fase 3).
 * Implementa normalización canónica de texto y resiliencia con fallback seguro.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MlInferenceServiceImpl implements MlInferenceService {

    private final RestClient mlRestClient;

    @Value("${ml.service.fallback-categoria:PAVIMENTACION}")
    private String fallbackCategoria;

    @Value("${ml.service.fallback-sentimiento:NEUTRAL}")
    private String fallbackSentimiento;

    @Value("${ml.service.fallback-urgencia:BAJA}")
    private String fallbackUrgencia;

    @Override
    public String predecirCategoria(String texto) {
        if (texto == null || texto.isBlank()) {
            log.warn("Texto vacío o nulo para inferencia de categoría. Usando categoría fallback: {}", fallbackCategoria);
            return fallbackCategoria;
        }

        try {
            log.debug("Enviando texto a FastAPI (/api/v1/clasificar/categoria) para inferencia de categoría...");
            CategoriaMlResponseDTO response = mlRestClient.post()
                    .uri("/api/v1/clasificar/categoria")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CategoriaMlRequestDTO(texto.trim()))
                    .retrieve()
                    .body(CategoriaMlResponseDTO.class);

            if (response != null && response.categoria() != null && !response.categoria().isBlank()) {
                String categoriaNormalizada = normalizarTexto(response.categoria(), fallbackCategoria);
                log.info("Inferencia exitosa desde FastAPI -> Categoría: '{}', Confianza: {}, Tiempo: {} ms",
                        categoriaNormalizada, response.confianza(), response.tiempo_ms());
                return categoriaNormalizada;
            } else {
                log.warn("Respuesta vacía del microservicio ML para categoría. Aplicando fallback: {}", fallbackCategoria);
                return fallbackCategoria;
            }
        } catch (RestClientException ex) {
            log.warn("Microservicio Python FastAPI no disponible o fuera de tiempo al clasificar categoría ({}). " +
                     "Aplicando fallback seguro a '{}'", ex.getMessage(), fallbackCategoria);
            return fallbackCategoria;
        } catch (Exception ex) {
            log.error("Error inesperado al invocar modelo de categoría: {}. Aplicando fallback: {}",
                    ex.getMessage(), fallbackCategoria, ex);
            return fallbackCategoria;
        }
    }

    @Override
    public String predecirSentimiento(String texto) {
        if (texto == null || texto.isBlank()) {
            log.warn("Texto vacío o nulo para inferencia de sentimiento. Usando sentimiento fallback: {}", fallbackSentimiento);
            return fallbackSentimiento;
        }

        try {
            log.debug("Enviando texto a FastAPI (/api/v1/clasificar/sentimiento) para inferencia de sentimiento...");
            SentimientoMlResponseDTO response = mlRestClient.post()
                    .uri("/api/v1/clasificar/sentimiento")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SentimientoMlRequestDTO(texto.trim()))
                    .retrieve()
                    .body(SentimientoMlResponseDTO.class);

            if (response != null && response.sentimiento() != null && !response.sentimiento().isBlank()) {
                String sentimientoNormalizado = normalizarTexto(response.sentimiento(), fallbackSentimiento);
                log.info("Inferencia exitosa desde FastAPI -> Sentimiento: '{}', Confianza: {}, Tiempo: {} ms",
                        sentimientoNormalizado, response.confianza(), response.tiempo_ms());
                return sentimientoNormalizado;
            } else {
                log.warn("Respuesta vacía del microservicio ML para sentimiento. Aplicando fallback: {}", fallbackSentimiento);
                return fallbackSentimiento;
            }
        } catch (RestClientException ex) {
            log.warn("Microservicio Python FastAPI no disponible o fuera de tiempo al clasificar sentimiento ({}). " +
                     "Aplicando fallback seguro a '{}'", ex.getMessage(), fallbackSentimiento);
            return fallbackSentimiento;
        } catch (Exception ex) {
            log.error("Error inesperado al invocar modelo de sentimiento: {}. Aplicando fallback: {}",
                    ex.getMessage(), fallbackSentimiento, ex);
            return fallbackSentimiento;
        }
    }

    @Override
    public UrgenciaMlResponseDTO predecirUrgencia(String texto, String categoria) {
        if (texto == null || texto.isBlank()) {
            log.warn("Texto vacío o nulo para inferencia de urgencia. Usando urgencia fallback: {}", fallbackUrgencia);
            return fallbackUrgenciaDTO();
        }

        try {
            log.debug("Enviando texto a FastAPI (/api/v1/clasificar/urgencia) para inferencia de urgencia y riesgo...");
            UrgenciaMlRequestDTO request = new UrgenciaMlRequestDTO(
                    texto.trim(),
                    categoria != null && !categoria.isBlank() ? categoria.trim() : null
            );

            UrgenciaMlResponseDTO response = mlRestClient.post()
                    .uri("/api/v1/clasificar/urgencia")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(UrgenciaMlResponseDTO.class);

            if (response != null && response.urgenciaMl() != null && !response.urgenciaMl().isBlank()) {
                String urgenciaMlNorm = normalizarTexto(response.urgenciaMl(), fallbackUrgencia);
                String nivelRiesgoNorm = normalizarTexto(response.nivelRiesgo(), "NINGUNO");
                String urgenciaFinalNorm = normalizarTexto(response.urgenciaFinal(), fallbackUrgencia);
                Boolean huboAjusteNorm = Boolean.TRUE.equals(response.huboAjuste());

                log.info("Inferencia exitosa desde FastAPI -> Urgencia ML: '{}', Riesgo: '{}', Final: '{}', Ajuste: {}, Confianza: {}, Tiempo: {} ms",
                        urgenciaMlNorm, nivelRiesgoNorm, urgenciaFinalNorm, huboAjusteNorm, response.confianza(), response.tiempoMs());

                return new UrgenciaMlResponseDTO(
                        urgenciaMlNorm,
                        nivelRiesgoNorm,
                        urgenciaFinalNorm,
                        huboAjusteNorm,
                        response.confianza(),
                        response.tiempoMs(),
                        response.urgenciaMlOriginal(),
                        response.urgenciaFinalOriginal()
                );
            } else {
                log.warn("Respuesta vacía del microservicio ML para urgencia. Aplicando fallback: {}", fallbackUrgencia);
                return fallbackUrgenciaDTO();
            }
        } catch (RestClientException ex) {
            log.warn("Microservicio Python FastAPI no disponible o fuera de tiempo al clasificar urgencia ({}). " +
                     "Aplicando fallback seguro a '{}'", ex.getMessage(), fallbackUrgencia);
            return fallbackUrgenciaDTO();
        } catch (Exception ex) {
            log.error("Error inesperado al invocar modelo de urgencia: {}. Aplicando fallback: {}",
                    ex.getMessage(), fallbackUrgencia, ex);
            return fallbackUrgenciaDTO();
        }
    }

    private UrgenciaMlResponseDTO fallbackUrgenciaDTO() {
        return new UrgenciaMlResponseDTO(
                fallbackUrgencia,
                "NINGUNO",
                fallbackUrgencia,
                false,
                0.0,
                0.0,
                fallbackUrgencia,
                fallbackUrgencia
        );
    }

    /**
     * Remueve acentos y caracteres diacríticos (ej. FRUSTRACIÓN -> FRUSTRACION)
     * y convierte a mayúsculas para coincidir exactamente con los catálogos en MySQL.
     */
    private String normalizarTexto(String valor, String defaultVal) {
        if (valor == null || valor.isBlank()) {
            return defaultVal;
        }
        String descompuesto = Normalizer.normalize(valor, Normalizer.Form.NFD);
        String sinTildes = descompuesto.replaceAll("\\p{M}", "");
        return sinTildes.trim().toUpperCase();
    }
}
