package com.gestionreportes.dashboard.config;

import com.gestionreportes.dashboard.model.*;
import com.gestionreportes.dashboard.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CategoriaRepository categoriaRepository;
    private final UrgenciaRepository urgenciaRepository;
    private final SentimientoRepository sentimientoRepository;
    private final EstadoReporteRepository estadoReporteRepository;
    private final ReporteRepository reporteRepository;
    private final OrdenTrabajoRepository ordenTrabajoRepository;
    private final HistorialReporteRepository historialReporteRepository;

    @Override
    public void run(String... args) {
        if (categoriaRepository.count() > 0) {
            log.info("Los catálogos ya se encuentran inicializados en la base de datos.");
            return;
        }

        log.info("Inicializando catálogos y datos de prueba según los scripts SQL...");

        // 1. Categorías
        Categoria pavimentacion = categoriaRepository.save(Categoria.builder()
                .nombre("PAVIMENTACION")
                .descripcion("Fallas estructurales en la superficie de rodado vial y peatonal: baches, grietas, hundimientos de aceras y reductores de velocidad en mal estado.")
                .build());

        Categoria alcantarillado = categoriaRepository.save(Categoria.builder()
                .nombre("ALCANTARILLADO")
                .descripcion("Anomalías en la red secundaria de distribución hídrica y recolección de aguas servidas visibles en la vía pública: tuberías rotas, filtraciones, inundaciones y tapas faltantes o dañadas.")
                .build());

        Categoria iluminacion = categoriaRepository.save(Categoria.builder()
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

        // 4. Estados de Reporte (orden definitivo 1 al 4)
        EstadoReporte estadoNoIniciado = estadoReporteRepository.save(EstadoReporte.builder().nombre("NO INICIADO").orden(1).build());
        EstadoReporte estadoIniciado = estadoReporteRepository.save(EstadoReporte.builder().nombre("INICIADO").orden(2).build());
        estadoReporteRepository.save(EstadoReporte.builder().nombre("EN REVISION").orden(3).build());
        estadoReporteRepository.save(EstadoReporte.builder().nombre("FINALIZADO").orden(4).build());

        // 5. Reportes de prueba (alineados con Poblar_base_script.sql)
        Reporte r1 = reporteRepository.save(Reporte.builder()
                .fechaIngreso(LocalDateTime.of(2026, 10, 15, 8, 30))
                .textoReporte("Existe un bache profundo en Avenida Libertad con Calle 4. Varios vehículos han reventado neumáticos y está cerca de una curva ciega.")
                .direccion("Av. Libertad 1240, Sector Centro")
                .categoria(pavimentacion)
                .sentimiento(sentimientoEnojado)
                .urgenciaMl(urgenciaMedia)
                .nivelRiesgo(NivelRiesgo.ALTO)
                .urgenciaFinal(urgenciaAlta)
                .huboAjuste(true)
                .estado(estadoNoIniciado)
                .build());

        Reporte r2 = reporteRepository.save(Reporte.builder()
                .fechaIngreso(LocalDateTime.of(2026, 10, 16, 11, 15))
                .textoReporte("Tapa de cámara de alcantarillado rota y abierta frente a la escuela básica. Emana agua servida y hay riesgo inminente de caída peatonal.")
                .direccion("Calle Los Aromos frente al N° 450")
                .categoria(alcantarillado)
                .sentimiento(sentimientoFrustrado)
                .urgenciaMl(urgenciaMedia)
                .nivelRiesgo(NivelRiesgo.CRITICO)
                .urgenciaFinal(urgenciaAlta)
                .huboAjuste(true)
                .estado(estadoIniciado)
                .build());

        Reporte r3 = reporteRepository.save(Reporte.builder()
                .fechaIngreso(LocalDateTime.of(2026, 10, 17, 19, 40))
                .textoReporte("Una de las luminarias de la plaza vecinal parpadea por momentos. La visibilidad general sigue siendo adecuada.")
                .direccion("Plaza Las Lilas, Pasaje Oriente")
                .categoria(iluminacion)
                .sentimiento(sentimientoNeutral)
                .urgenciaMl(urgenciaBaja)
                .nivelRiesgo(NivelRiesgo.NINGUNO)
                .urgenciaFinal(urgenciaBaja)
                .huboAjuste(false)
                .estado(estadoNoIniciado)
                .build());

        // 6. Orden de trabajo asociada al Reporte 2 (estado INICIADO)
        ordenTrabajoRepository.save(OrdenTrabajo.builder()
                .reporte(r2)
                .cuadrilla("Cuadrilla de Emergencias Sanitarias")
                .responsable("Carlos Méndez")
                .cantidadPersonal((short) 4)
                .vehiculo("Camioneta Técnica OP-07")
                .fechaVisita(LocalDate.of(2026, 10, 17))
                .horaVisita(LocalTime.of(9, 0))
                .duracionEstimada("3 horas")
                .materiales("Tapa de cámara de hierro fundido, señalética reflectante y vallas de seguridad.")
                .instrucciones("Asegurar el perímetro escolar, retirar restos de la tapa dañada e instalar reposición definitiva.")
                .build());

        // 7. Historial inicial (trazabilidad)
        historialReporteRepository.save(HistorialReporte.builder()
                .reporte(r1)
                .estado(estadoNoIniciado)
                .fechaCambio(LocalDateTime.of(2026, 10, 15, 8, 30))
                .observacion("Reporte ingresado y clasificado automáticamente por el sistema.")
                .build());

        historialReporteRepository.save(HistorialReporte.builder()
                .reporte(r2)
                .estado(estadoNoIniciado)
                .fechaCambio(LocalDateTime.of(2026, 10, 16, 11, 15))
                .observacion("Reporte ingresado y clasificado automáticamente con alerta crítica.")
                .build());

        historialReporteRepository.save(HistorialReporte.builder()
                .reporte(r2)
                .estado(estadoIniciado)
                .fechaCambio(LocalDateTime.of(2026, 10, 16, 11, 45))
                .observacion("Orden de trabajo emitida y asignada a Cuadrilla de Emergencias Sanitarias.")
                .build());

        historialReporteRepository.save(HistorialReporte.builder()
                .reporte(r3)
                .estado(estadoNoIniciado)
                .fechaCambio(LocalDateTime.of(2026, 10, 17, 19, 40))
                .observacion("Reporte ingresado y clasificado automáticamente por el sistema.")
                .build());

        log.info("Datos iniciales cargados exitosamente.");
    }
}
