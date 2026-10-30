package com.gestionreportes.dashboard.config;

import com.gestionreportes.dashboard.model.*;
import com.gestionreportes.dashboard.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CategoriaRepository categoriaRepository;
    private final UrgenciaRepository urgenciaRepository;
    private final SentimientoRepository sentimientoRepository;
    private final EstadoReporteRepository estadoReporteRepository;
    private final ReporteRepository reporteRepository;
    private final HistorialReporteRepository historialReporteRepository;

    @Override
    public void run(String... args) {
        if (categoriaRepository.count() > 0) {
            log.info("Los catálogos ya se encuentran inicializados en la base de datos.");
            return;
        }

        log.info("Inicializando catálogos y datos de prueba según el script SQL...");

        // 1. Categorías
        Categoria pavimentacion = categoriaRepository.save(Categoria.builder()
                .nombre("PAVIMENTACION")
                .descripcion("Fallas estructurales en la superficie de rodado vial y peatonal: baches, grietas, hundimientos de aceras y reductores de velocidad en mal estado.")
                .build());

        categoriaRepository.save(Categoria.builder()
                .nombre("ALCANTARILLADO")
                .descripcion("Anomalías en la red secundaria de distribución hídrica y recolección de aguas servidas visibles en la vía pública: tuberías rotas, filtraciones, inundaciones y tapas faltantes o dañadas.")
                .build());

        categoriaRepository.save(Categoria.builder()
                .nombre("ILUMINACION")
                .descripcion("Deficiencias en el alumbrado público que comprometen la visibilidad nocturna: luminarias apagadas, focos quemados, postes dañados o ausencia de alumbrado.")
                .build());

        // 2. Urgencias
        Urgencia urgenciaBaja = urgenciaRepository.save(Urgencia.builder().nombre("BAJA").nivel(0).build());
        Urgencia urgenciaMedia = urgenciaRepository.save(Urgencia.builder().nombre("MEDIA").nivel(1).build());
        Urgencia urgenciaAlta = urgenciaRepository.save(Urgencia.builder().nombre("ALTA").nivel(2).build());

        // 3. Sentimientos
        Sentimiento sentimientoEnojado = sentimientoRepository.save(Sentimiento.builder().nombre("ENOJADO").build());
        Sentimiento sentimientoNeutral = sentimientoRepository.save(Sentimiento.builder().nombre("NEUTRAL").build());
        Sentimiento sentimientoFrustrado = sentimientoRepository.save(Sentimiento.builder().nombre("FRUSTRADO").build());

        // 4. Estados de Reporte
        estadoReporteRepository.save(EstadoReporte.builder().nombre("NO INICIADO").orden(0).build());
        EstadoReporte estadoIniciado = estadoReporteRepository.save(EstadoReporte.builder().nombre("INICIADO").orden(1).build());
        EstadoReporte estadoEnProceso = estadoReporteRepository.save(EstadoReporte.builder().nombre("EN PROCESO").orden(2).build());
        EstadoReporte estadoFinalizado = estadoReporteRepository.save(EstadoReporte.builder().nombre("FINALIZADO").orden(3).build());

        // 5. Reportes de demostración
        Reporte r1 = reporteRepository.save(Reporte.builder()
                .fechaIngreso(LocalDateTime.of(2026, 11, 3, 10, 30))
                .textoReporte("Gran bache en la intersección de Av. Libertad con Calle 5, está causando daños a los vehículos y representa un peligro para los ciclistas que transitan diariamente.")
                .direccion("Av. Libertad con Calle 5")
                .categoria(pavimentacion)
                .sentimiento(sentimientoEnojado)
                .urgenciaMl(urgenciaAlta)
                .urgenciaFinal(urgenciaAlta)
                .nivelRiesgo(NivelRiesgo.ALTO)
                .huboAjuste(false)
                .estado(estadoEnProceso)
                .build());

        // Trazabilidad para el Reporte 1
        historialReporteRepository.save(HistorialReporte.builder()
                .reporte(r1)
                .estado(estadoIniciado)
                .fechaCambio(LocalDateTime.of(2026, 11, 3, 10, 30))
                .observacion("Reporte recibido y registrado en el sistema.")
                .build());

        historialReporteRepository.save(HistorialReporte.builder()
                .reporte(r1)
                .estado(estadoEnProceso)
                .fechaCambio(LocalDateTime.of(2026, 11, 5, 14, 15))
                .observacion("Cuadrilla asignada. Reparación programada para el 10/11.")
                .build());

        Reporte r2 = reporteRepository.save(Reporte.builder()
                .fechaIngreso(LocalDateTime.of(2026, 11, 5, 9, 0))
                .textoReporte("El pavimento frente al mercado municipal presenta hundimientos y grietas profundas que dificultan el tránsito.")
                .direccion("Frente al mercado municipal")
                .categoria(pavimentacion)
                .sentimiento(sentimientoFrustrado)
                .urgenciaMl(urgenciaMedia)
                .urgenciaFinal(urgenciaMedia)
                .nivelRiesgo(NivelRiesgo.NINGUNO)
                .huboAjuste(false)
                .estado(estadoEnProceso)
                .build());

        historialReporteRepository.save(HistorialReporte.builder()
                .reporte(r2)
                .estado(estadoIniciado)
                .fechaCambio(LocalDateTime.of(2026, 11, 5, 9, 0))
                .observacion("Reporte ingresado vía canal web.")
                .build());

        Reporte r3 = reporteRepository.save(Reporte.builder()
                .fechaIngreso(LocalDateTime.of(2026, 10, 28, 16, 45))
                .textoReporte("La calle peatonal en el barrio Las Flores fue reparada hace dos semanas y la comunidad está conforme.")
                .direccion("Barrio Las Flores")
                .categoria(pavimentacion)
                .sentimiento(sentimientoNeutral)
                .urgenciaMl(urgenciaBaja)
                .urgenciaFinal(urgenciaBaja)
                .nivelRiesgo(NivelRiesgo.NINGUNO)
                .huboAjuste(false)
                .estado(estadoFinalizado)
                .build());

        historialReporteRepository.save(HistorialReporte.builder()
                .reporte(r3)
                .estado(estadoFinalizado)
                .fechaCambio(LocalDateTime.of(2026, 11, 1, 12, 0))
                .observacion("Trabajos concluidos e inspección técnica aprobada.")
                .build());

        log.info("Datos iniciales cargados exitosamente.");
    }
}
