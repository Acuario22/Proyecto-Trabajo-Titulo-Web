package com.gestionreportes.dashboard;

import com.gestionreportes.dashboard.dto.ml.UrgenciaMlResponseDTO;
import com.gestionreportes.dashboard.dto.request.CrearReporteRequestDTO;
import com.gestionreportes.dashboard.dto.response.ReporteDTO;
import com.gestionreportes.dashboard.service.MlInferenceService;
import com.gestionreportes.dashboard.service.ReporteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DashboardAdministrativoBackendApplicationTests {

    @Autowired
    private MlInferenceService mlInferenceService;

    @Autowired
    private ReporteService reporteService;

    private static final Set<String> CATEGORIAS_VALIDAS = Set.of("PAVIMENTACION", "ALCANTARILLADO", "ILUMINACION");
    private static final Set<String> SENTIMIENTOS_VALIDOS = Set.of("ENOJADO", "NEUTRAL", "FRUSTRADO");

    // ==========================================
    // FASE 1: Clasificador de Categoría
    // ==========================================
    @Test
    void testPredecirCategoriaSemaforos() {
        String texto = "¡Habiliten esos semáforos de la avenida Libertador! Se han quedado sin función desde hace tres semanas. No es solo molesto, sino que hay gente que se atreve a cruzar con el alto y las personas mayores andan nerviosas.";
        String cat = mlInferenceService.predecirCategoria(texto);
        System.out.println(">>> RESULTADO PREDICCION SEMAFOROS: [" + cat + "]");
        assertEquals("ILUMINACION", cat);
        assertTrue(CATEGORIAS_VALIDAS.contains(cat));
    }

    @Test
    void testPredecirCategoriaBache() {
        String texto = "En la avenida Libertador, entre las calles Ercilla y San Martín, hay un bache tan grande que da miedo cruzar en bicicleta.";
        String cat = mlInferenceService.predecirCategoria(texto);
        System.out.println(">>> RESULTADO PREDICCION BACHE: [" + cat + "]");
        assertEquals("PAVIMENTACION", cat);
        assertTrue(CATEGORIAS_VALIDAS.contains(cat));
    }

    @Test
    void testPredecirCategoriaAlcantarilla() {
        String texto = "Hay una fuga de agua servida y alcantarilla rebasada con olor nauseabundo en la esquina.";
        String cat = mlInferenceService.predecirCategoria(texto);
        System.out.println(">>> RESULTADO PREDICCION ALCANTARILLA: [" + cat + "]");
        assertEquals("ALCANTARILLADO", cat);
        assertTrue(CATEGORIAS_VALIDAS.contains(cat));
    }

    // ==========================================
    // FASE 2: Clasificador de Sentimiento
    // ==========================================
    @Test
    void testPredecirSentimientoFrustrado() {
        String texto = "Ya hace un mes que se rompio la tapa de la camara y nadie ha venido a repararlo, estamos cansados de esperar";
        String sent = mlInferenceService.predecirSentimiento(texto);
        System.out.println(">>> RESULTADO PREDICCION FRUSTRADO: [" + sent + "]");
        assertEquals("FRUSTRADO", sent);
        assertTrue(SENTIMIENTOS_VALIDOS.contains(sent));
    }

    @Test
    void testPredecirSentimientoEnojado() {
        String texto = "¡Esto es una vergüenza inaceptable! ¡Pésima gestión municipal, exijo una solución inmediata!";
        String sent = mlInferenceService.predecirSentimiento(texto);
        System.out.println(">>> RESULTADO PREDICCION ENOJADO: [" + sent + "]");
        assertEquals("ENOJADO", sent);
        assertTrue(SENTIMIENTOS_VALIDOS.contains(sent));
    }

    // ==========================================
    // FASE 3: Clasificador de Urgencia + Motor de Priorización
    // ==========================================
    private static final Set<String> URGENCIAS_VALIDAS = Set.of("BAJA", "MEDIA", "ALTA");
    private static final Set<String> RIESGOS_VALIDOS = Set.of("CRITICO", "ALTO", "NINGUNO");

    @Test
    void testPredecirUrgenciaRiesgoCritico() {
        String texto = "Hay un cable energizado cortado en el suelo echando chispas, peligro de muerte y electrocucion";
        UrgenciaMlResponseDTO res = mlInferenceService.predecirUrgencia(texto, "ILUMINACION");

        System.out.println(">>> RESULTADO URGENCIA CRITICO: " + res);
        assertNotNull(res);
        assertEquals("ALTA", res.urgenciaFinal());
        assertEquals("CRITICO", res.nivelRiesgo());
        assertTrue(URGENCIAS_VALIDAS.contains(res.urgenciaFinal()));
        assertTrue(RIESGOS_VALIDOS.contains(res.nivelRiesgo()));
        assertNotNull(res.confianza());
    }

    @Test
    void testPredecirUrgenciaAjustePorRiesgoAlto() {
        String texto = "En la villa El Bosque hay un hoyo en el pavimento que esta mal nivelado, es peligroso para los carros";
        UrgenciaMlResponseDTO res = mlInferenceService.predecirUrgencia(texto, "PAVIMENTACION");

        System.out.println(">>> RESULTADO URGENCIA ESCALADO POR RIESGO: " + res);
        assertNotNull(res);
        assertEquals("BAJA", res.urgenciaMl());
        assertEquals("ALTO", res.nivelRiesgo());
        assertEquals("MEDIA", res.urgenciaFinal());
        assertTrue(res.huboAjuste(), "Debe haber ajuste de BAJA a MEDIA debido a riesgo ALTO");
    }

    @Test
    void testPredecirUrgenciaNormalSinRiesgo() {
        String texto = "Pintaron una demarcacion en la esquina pero la pintura quedo un poco corrida";
        UrgenciaMlResponseDTO res = mlInferenceService.predecirUrgencia(texto, "PAVIMENTACION");

        System.out.println(">>> RESULTADO URGENCIA NORMAL: " + res);
        assertNotNull(res);
        assertEquals("MEDIA", res.urgenciaMl());
        assertEquals("NINGUNO", res.nivelRiesgo());
        assertEquals("MEDIA", res.urgenciaFinal());
        assertFalse(res.huboAjuste(), "No debe haber ajuste de urgencia");
    }

    // ==========================================
    // INTEGRACIÓN END-TO-END: Crear Reporte con Inferencia Automática
    // ==========================================
    @Test
    @Transactional
    void testCrearReporteConInferenciaAutomaticaCompleta() {
        CrearReporteRequestDTO request = new CrearReporteRequestDTO();
        request.setTextoReporte("Hay un cable energizado cortado en el suelo echando chispas, peligro de muerte y electrocucion en la plaza central");
        request.setDireccion("Plaza Central #123");

        ReporteDTO creado = reporteService.crearReporte(request);

        System.out.println(">>> REPORTE CREADO E2E: " + creado);
        assertNotNull(creado);
        assertNotNull(creado.getId());
        assertEquals("Iluminacion", creado.getCategoria());
        assertEquals("Alta", creado.getUrgencia());
        assertEquals("CRITICO", creado.getNivelRiesgo());
        assertNotNull(creado.getSentimiento());
    }

    @Test
    @Transactional
    void testCrearReporteConAjusteDeRiesgoAlto() {
        CrearReporteRequestDTO request = new CrearReporteRequestDTO();
        request.setTextoReporte("En la villa El Bosque hay un hoyo en el pavimento que esta mal nivelado, es peligroso para los carros");
        request.setDireccion("Villa El Bosque Pasaje 2");

        ReporteDTO creado = reporteService.crearReporte(request);

        System.out.println(">>> REPORTE CON AJUSTE E2E: " + creado);
        assertNotNull(creado);
        assertNotNull(creado.getId());
        assertEquals("Pavimentacion", creado.getCategoria());
        assertEquals("Media", creado.getUrgencia());
        assertEquals("ALTO", creado.getNivelRiesgo());
        assertTrue(creado.getHuboAjuste());
    }
}
