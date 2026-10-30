package com.gestionreportes.dashboard.service.impl;

import com.gestionreportes.dashboard.dto.ml.UrgenciaMlResponseDTO;
import com.gestionreportes.dashboard.dto.request.CerrarReporteRequestDTO;
import com.gestionreportes.dashboard.dto.request.CrearOrdenTrabajoRequestDTO;
import com.gestionreportes.dashboard.dto.request.CrearReporteRequestDTO;
import com.gestionreportes.dashboard.dto.response.*;
import com.gestionreportes.dashboard.model.*;
import com.gestionreportes.dashboard.repository.*;
import com.gestionreportes.dashboard.service.MlInferenceService;
import com.gestionreportes.dashboard.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteServiceImpl implements ReporteService {

        private final ReporteRepository reporteRepository;
        private final CategoriaRepository categoriaRepository;
        private final UrgenciaRepository urgenciaRepository;
        private final SentimientoRepository sentimientoRepository;
        private final EstadoReporteRepository estadoReporteRepository;
        private final HistorialReporteRepository historialReporteRepository;
        private final OrdenTrabajoRepository ordenTrabajoRepository;
        private final MlInferenceService mlInferenceService;

        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        @Override
        public List<ReporteDTO> listarReportes(String categoria) {
                return listarReportes(categoria, null);
        }

        @Override
        public List<ReporteDTO> listarReportes(String categoria, Integer idEstado) {
                boolean tieneCategoria = categoria != null && !categoria.trim().isEmpty()
                                && !categoria.equalsIgnoreCase("TODOS");
                boolean tieneEstado = idEstado != null && idEstado > 0;

                List<Reporte> reportes;
                if (tieneCategoria && tieneEstado) {
                        reportes = reporteRepository.findAllWithRelationsByCategoriaAndEstado(categoria.trim(),
                                        idEstado);
                } else if (tieneCategoria) {
                        reportes = reporteRepository.findAllWithRelationsByCategoria(categoria.trim());
                } else if (tieneEstado) {
                        reportes = reporteRepository.findAllWithRelationsByEstado(idEstado);
                } else {
                        reportes = reporteRepository.findAllWithRelations();
                }

                return reportes.stream()
                                .map(this::mapearAResumenDTO)
                                .collect(Collectors.toList());
        }

        @Override
        public List<ReporteDTO> listarPorEstado(Integer idEstado) {
                return listarReportes(null, idEstado);
        }

        @Override
        public ReporteDTO obtenerReportePorId(Integer id) {
                // findByIdWithRelations usa JOIN FETCH para cargar todas las relaciones
                // en una única consulta SQL, evitando el problema N+1.
                Reporte reporte = reporteRepository.findByIdWithRelations(id)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Reporte no encontrado con ID: " + id));
                return mapearAResumenDTO(reporte);
        }

        @Override
        public List<CategoriaConteoDTO> listarCategoriasConConteo() {
                return categoriaRepository.findAll().stream()
                                .map(cat -> CategoriaConteoDTO.builder()
                                                .nombre(formatearCapital(cat.getNombre()))
                                                .conteo(reporteRepository
                                                                .countByCategoriaNombreIgnoreCase(cat.getNombre()))
                                                .build())
                                .collect(Collectors.toList());
        }

        @Override
        public DashboardMetricasDTO obtenerMetricas() {
                long total = reporteRepository.count();
                long urgenciaAlta = reporteRepository.countByUrgenciaFinalNombreIgnoreCase("ALTA");
                long finalizados = reporteRepository.countByEstadoNombreIgnoreCase("FINALIZADO");

                return DashboardMetricasDTO.builder()
                                .totalReportes(total)
                                .urgenciaAlta(urgenciaAlta)
                                .finalizados(finalizados)
                                .build();
        }

        @Override
        public TrazabilidadReporteDTO obtenerTrazabilidad(String identificador) {
                Integer idReporte = parsearIdentificador(identificador);

                Reporte reporte = reporteRepository.findById(idReporte)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Reporte no encontrado: " + identificador));

                List<HistorialReporte> historial = historialReporteRepository
                                .findByReporteIdOrderByFechaCambioAsc(idReporte);

                int ordenActual = reporte.getEstado() != null ? reporte.getEstado().getOrden() : 0;

                // Construir los 3 pasos principales visuales (Iniciado = 1, En Proceso = 2,
                // Finalizado = 3)
                List<PasoLineaTiempoDTO> pasos = new ArrayList<>();

                // Paso 1: Iniciado (orden 1)
                pasos.add(construirPaso(1, "Iniciado", ordenActual, 1, historial,
                                "Reporte recibido y registrado en el sistema."));

                // Paso 2: En Proceso (orden 2)
                pasos.add(construirPaso(2, "En Proceso", ordenActual, 2, historial,
                                "Cuadrilla asignada. Reparación programada."));

                // Paso 3: Finalizado (orden 3)
                pasos.add(construirPaso(3, "Finalizado", ordenActual, 3, historial,
                                "Pendiente de ejecución."));

                String categoriaFormateada = reporte.getCategoria() != null
                                ? formatearCapital(reporte.getCategoria().getNombre())
                                : "General";
                String estadoActualNombre = reporte.getEstado() != null
                                ? formatearCapital(reporte.getEstado().getNombre())
                                : "No Iniciado";

                // Obtener última observación del historial o descripción por defecto
                String detalleActual = historial.isEmpty() ? "En espera de asignación de cuadrilla."
                                : historial.get(historial.size() - 1).getObservacion();
                if (detalleActual == null || detalleActual.isBlank()) {
                        detalleActual = "Gestión en curso según el protocolo operativo.";
                }

                return TrazabilidadReporteDTO.builder()
                                .codigo(formatearCodigo(reporte))
                                .titulo("Reporte de " + categoriaFormateada)
                                .descripcion(reporte.getTextoReporte())
                                .direccion(reporte.getDireccion())
                                .fechaIngreso(reporte.getFechaIngreso().format(DATE_FORMATTER))
                                .urgencia(reporte.getUrgenciaFinal() != null
                                                ? formatearCapital(reporte.getUrgenciaFinal().getNombre())
                                                : "Baja")
                                .sentimiento(reporte.getSentimiento() != null
                                                ? formatearCapital(reporte.getSentimiento().getNombre())
                                                : "Neutral")
                                .estadoActual(estadoActualNombre)
                                .detalleEstadoActual(detalleActual)
                                .pasos(pasos)
                                .build();
        }

        @Override
        @Transactional
        public ReporteDTO crearReporte(CrearReporteRequestDTO request) {
                // a) Resolver Categoría: flujo manual por ID (> 0) o automático por modelo ML
                Categoria categoria;
                if (request.getIdCategoria() != null && request.getIdCategoria() > 0) {
                        categoria = categoriaRepository.findById(request.getIdCategoria())
                                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                        "Categoría no válida con ID: " + request.getIdCategoria()));
                } else {
                        String nombreCategoria = mlInferenceService.predecirCategoria(request.getTextoReporte());
                        categoria = categoriaRepository.findByNombreIgnoreCase(nombreCategoria)
                                        .orElseGet(() -> categoriaRepository.findByNombreIgnoreCase("PAVIMENTACION")
                                                        .orElseGet(() -> categoriaRepository.findAll().stream()
                                                                        .findFirst()
                                                                        .orElseThrow(() -> new ResponseStatusException(
                                                                                        HttpStatus.INTERNAL_SERVER_ERROR,
                                                                                        "No hay categorías configuradas"))));
                }

                // b) Resolver Sentimiento (Fase 2: manual por ID o automático por modelo ML)
                Sentimiento sentimiento;
                if (request.getIdSentimiento() != null && request.getIdSentimiento() > 0) {
                        sentimiento = sentimientoRepository.findById(request.getIdSentimiento())
                                        .orElseGet(this::obtenerSentimientoDefault);
                } else {
                        String nombreSentimiento = mlInferenceService.predecirSentimiento(request.getTextoReporte());
                        sentimiento = sentimientoRepository.findByNombreIgnoreCase(nombreSentimiento)
                                        .orElseGet(this::obtenerSentimientoDefault);
                }

                // c) Resolver Urgencia y Reglas de Priorización (Fase 3: SVM Lineal + Motor de
                // Reglas)
                UrgenciaMlResponseDTO urgenciaResultado = null;
                boolean requiereInferenciaUrgencia = (request.getIdUrgenciaMl() == null
                                || request.getIdUrgenciaMl() <= 0)
                                || (request.getIdUrgenciaFinal() == null || request.getIdUrgenciaFinal() <= 0);

                if (requiereInferenciaUrgencia) {
                        urgenciaResultado = mlInferenceService.predecirUrgencia(request.getTextoReporte(),
                                        categoria.getNombre());
                }

                // c.1) Urgencia ML (predicción pura del modelo de ML)
                Urgencia urgenciaMl;
                if (request.getIdUrgenciaMl() != null && request.getIdUrgenciaMl() > 0) {
                        urgenciaMl = urgenciaRepository.findById(request.getIdUrgenciaMl())
                                        .orElseGet(this::obtenerUrgenciaDefault);
                } else if (urgenciaResultado != null && urgenciaResultado.urgenciaMl() != null) {
                        urgenciaMl = urgenciaRepository.findByNombreIgnoreCase(urgenciaResultado.urgenciaMl())
                                        .orElseGet(this::obtenerUrgenciaDefault);
                } else {
                        urgenciaMl = obtenerUrgenciaDefault();
                }

                // c.2) Urgencia Final (posible ajuste por motor determinístico de reglas de
                // riesgo)
                Urgencia urgenciaFinal;
                if (request.getIdUrgenciaFinal() != null && request.getIdUrgenciaFinal() > 0) {
                        urgenciaFinal = urgenciaRepository.findById(request.getIdUrgenciaFinal())
                                        .orElseGet(this::obtenerUrgenciaDefault);
                } else if (urgenciaResultado != null && urgenciaResultado.urgenciaFinal() != null) {
                        urgenciaFinal = urgenciaRepository.findByNombreIgnoreCase(urgenciaResultado.urgenciaFinal())
                                        .orElseGet(this::obtenerUrgenciaDefault);
                } else {
                        urgenciaFinal = obtenerUrgenciaDefault();
                }

                // c.3) Nivel de Riesgo
                NivelRiesgo nivelRiesgo = NivelRiesgo.NINGUNO;
                try {
                        if (request.getNivelRiesgo() != null && !request.getNivelRiesgo().isBlank()) {
                                nivelRiesgo = NivelRiesgo.valueOf(request.getNivelRiesgo().toUpperCase());
                        } else if (urgenciaResultado != null && urgenciaResultado.nivelRiesgo() != null) {
                                nivelRiesgo = NivelRiesgo.valueOf(urgenciaResultado.nivelRiesgo().toUpperCase());
                        }
                } catch (IllegalArgumentException ignored) {
                }

                // c.4) Hubo Ajuste
                Boolean huboAjuste;
                if (request.getHuboAjuste() != null) {
                        huboAjuste = request.getHuboAjuste();
                } else if (urgenciaResultado != null && urgenciaResultado.huboAjuste() != null) {
                        huboAjuste = urgenciaResultado.huboAjuste();
                } else {
                        huboAjuste = false;
                }

                // d) Estado Inicial: ID_ESTADO = 1 (NO INICIADO)
                EstadoReporte estadoInicial = estadoReporteRepository.findById(1)
                                .orElseGet(() -> estadoReporteRepository.findByNombreIgnoreCase("NO INICIADO")
                                                .orElseGet(() -> estadoReporteRepository.findAllByOrderByOrdenAsc()
                                                                .stream().findFirst()
                                                                .orElseThrow(() -> new ResponseStatusException(
                                                                                HttpStatus.INTERNAL_SERVER_ERROR,
                                                                                "No hay estados configurados"))));

                Reporte nuevoReporte = Reporte.builder()
                                .fechaIngreso(LocalDateTime.now())
                                .textoReporte(request.getTextoReporte())
                                .direccion(request.getDireccion())
                                .categoria(categoria)
                                .sentimiento(sentimiento)
                                .urgenciaMl(urgenciaMl)
                                .urgenciaFinal(urgenciaFinal)
                                .nivelRiesgo(nivelRiesgo)
                                .huboAjuste(huboAjuste)
                                .estado(estadoInicial)
                                .build();

                Reporte guardado = reporteRepository.save(nuevoReporte);

                // e) Registro inicial en TBLHISTORIAL (Hito inicial: NO INICIADO)
                String detalleAjuste = Boolean.TRUE.equals(huboAjuste)
                                ? " (Ajustado por Riesgo " + nivelRiesgo + ")"
                                : (nivelRiesgo != NivelRiesgo.NINGUNO ? " (Riesgo " + nivelRiesgo + ")" : "");

                HistorialReporte historialInicial = HistorialReporte.builder()
                                .reporte(guardado)
                                .estado(estadoInicial)
                                .fechaCambio(LocalDateTime.now())
                                .observacion("Reporte recibido y registrado en el sistema. Clasificado como "
                                                + categoria.getNombre() + " (Sentimiento: " + sentimiento.getNombre()
                                                + ", Urgencia: " + urgenciaFinal.getNombre() + detalleAjuste + ").")
                                .build();
                historialReporteRepository.save(historialInicial);

                return mapearAResumenDTO(guardado);
        }

        @Override
        @Transactional
        public OrdenTrabajoResponseDTO crearOrdenTrabajo(Integer idReporte, CrearOrdenTrabajoRequestDTO request) {
                Reporte reporte = reporteRepository.findById(idReporte)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Reporte no encontrado con ID: " + idReporte));

                // Buscar si ya existe una orden de trabajo previa para este reporte (actualizar
                // o crear)
                OrdenTrabajo ordenTrabajo = ordenTrabajoRepository.findByReporte_IdReporte(idReporte)
                                .orElseGet(() -> OrdenTrabajo.builder()
                                                .reporte(reporte)
                                                .build());

                ordenTrabajo.setCuadrilla(request.getCuadrilla());
                ordenTrabajo.setResponsable(request.getResponsable());
                ordenTrabajo.setCantidadPersonal(
                                request.getCantidadPersonal() != null ? request.getCantidadPersonal().shortValue() : 1);
                ordenTrabajo.setVehiculo(request.getVehiculo());
                ordenTrabajo.setFechaVisita(request.getFechaVisita());
                ordenTrabajo.setHoraVisita(request.getHoraVisita());
                ordenTrabajo.setDuracionEstimada(request.getDuracionEstimada());
                ordenTrabajo.setMateriales(request.getMateriales());
                ordenTrabajo.setInstrucciones(request.getInstrucciones());

                OrdenTrabajo guardada = ordenTrabajoRepository.save(ordenTrabajo);

                // Actualizar estado del reporte a INICIADO (ID_ESTADO = 2)
                EstadoReporte estadoIniciado = estadoReporteRepository.findById(2)
                                .orElseGet(() -> estadoReporteRepository.findByNombreIgnoreCase("INICIADO")
                                                .orElseThrow(() -> new ResponseStatusException(
                                                                HttpStatus.INTERNAL_SERVER_ERROR,
                                                                "Estado 'INICIADO' no encontrado en el sistema")));

                reporte.setEstado(estadoIniciado);
                reporteRepository.save(reporte);

                // Insertar registro en TBLHISTORIAL
                HistorialReporte nuevoHistorial = HistorialReporte.builder()
                                .reporte(reporte)
                                .estado(estadoIniciado)
                                .fechaCambio(LocalDateTime.now())
                                .observacion("Orden de trabajo asignada a " + request.getCuadrilla()
                                                + " - Responsable: "
                                                + request.getResponsable())
                                .build();
                historialReporteRepository.save(nuevoHistorial);

                return mapearAOrdenTrabajoDTO(guardada, reporte);
        }

        @Override
        public OrdenTrabajoResponseDTO obtenerOrdenTrabajoPorReporte(Integer idReporte) {
                Reporte reporte = reporteRepository.findById(idReporte)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Reporte no encontrado con ID: " + idReporte));

                OrdenTrabajo ordenTrabajo = ordenTrabajoRepository.findByReporte_IdReporte(idReporte)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "No existe una orden de trabajo asociada al reporte ID: " + idReporte));

                return mapearAOrdenTrabajoDTO(ordenTrabajo, reporte);
        }

        @Override
        @Transactional
        public OrdenTrabajoResponseDTO finalizarReporte(Integer idReporte, CerrarReporteRequestDTO request) {
                // a) Buscar el reporte por idReporte
                Reporte reporte = reporteRepository.findById(idReporte)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Reporte no encontrado con ID: " + idReporte));

                // b) Buscar la orden de trabajo asociada en TBLORDENTRABAJO y registrar cierre
                OrdenTrabajo ordenTrabajo = ordenTrabajoRepository.findByReporte_IdReporte(idReporte)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "No existe una orden de trabajo asociada al reporte ID: " + idReporte));

                ordenTrabajo.setFechaCierreAdmin(LocalDateTime.now());
                ordenTrabajo.setObservacionCierreAdmin(request.getObservacionCierreAdmin());
                OrdenTrabajo guardada = ordenTrabajoRepository.save(ordenTrabajo);

                // c) Si el reporte aún no tenía un hito de estado 3 (EN REVISION) en
                // TBLHISTORIAL, insertarlo primero
                List<HistorialReporte> historial = historialReporteRepository
                                .findByReporteIdOrderByFechaCambioAsc(idReporte);
                boolean tieneEstadoRevision = historial.stream()
                                .anyMatch(h -> h.getEstado() != null && (Integer.valueOf(3)
                                                .equals(h.getEstado().getIdEstado())
                                                || "EN REVISION".equalsIgnoreCase(h.getEstado().getNombre())
                                                || "EN REVISIÓN".equalsIgnoreCase(h.getEstado().getNombre())));

                if (!tieneEstadoRevision) {
                        EstadoReporte estadoRevision = estadoReporteRepository.findById(3)
                                        .orElseGet(() -> estadoReporteRepository.findByNombreIgnoreCase("EN REVISION")
                                                        .orElseGet(() -> estadoReporteRepository
                                                                        .findByNombreIgnoreCase("EN REVISIÓN")
                                                                        .orElseGet(() -> estadoReporteRepository
                                                                                        .findByNombreIgnoreCase(
                                                                                                        "EN PROCESO")
                                                                                        .orElse(null))));

                        if (estadoRevision != null) {
                                String cuad = guardada.getCuadrilla() != null ? guardada.getCuadrilla()
                                                : "Cuadrilla asignada";
                                String resp = guardada.getResponsable() != null ? guardada.getResponsable()
                                                : "Responsable";
                                String comentarioCuadrilla = "Trabajo ejecutado en terreno por " + cuad + " a cargo de "
                                                + resp
                                                + ". Se cumplieron las instrucciones operativas y la zona quedó habilitada.";

                                HistorialReporte hRev = HistorialReporte.builder()
                                                .reporte(reporte)
                                                .estado(estadoRevision)
                                                .fechaCambio(LocalDateTime.now().minusMinutes(5))
                                                .observacion(comentarioCuadrilla)
                                                .build();
                                historialReporteRepository.save(hRev);
                        }
                }

                // d) Actualizar el estado del reporte en TBLREPORTES a ID_ESTADO = 4
                // (FINALIZADO)
                EstadoReporte estadoFinalizado = estadoReporteRepository.findById(4)
                                .orElseGet(() -> estadoReporteRepository.findByNombreIgnoreCase("FINALIZADO")
                                                .orElseThrow(() -> new ResponseStatusException(
                                                                HttpStatus.INTERNAL_SERVER_ERROR,
                                                                "Estado 'FINALIZADO' no encontrado en el sistema")));

                reporte.setEstado(estadoFinalizado);
                reporteRepository.save(reporte);

                // e) Insertar el registro final en TBLHISTORIAL
                HistorialReporte hFinal = HistorialReporte.builder()
                                .reporte(reporte)
                                .estado(estadoFinalizado)
                                .fechaCambio(LocalDateTime.now())
                                .observacion("Cierre aprobado por administración: "
                                                + request.getObservacionCierreAdmin())
                                .build();
                historialReporteRepository.save(hFinal);

                // f) Retornar DTO de la orden actualizada
                return mapearAOrdenTrabajoDTO(guardada, reporte);
        }

        private OrdenTrabajoResponseDTO mapearAOrdenTrabajoDTO(OrdenTrabajo orden, Reporte reporte) {
                String estadoNombre = (reporte != null && reporte.getEstado() != null)
                                ? formatearCapital(reporte.getEstado().getNombre())
                                : (orden.getReporte() != null && orden.getReporte().getEstado() != null)
                                                ? formatearCapital(orden.getReporte().getEstado().getNombre())
                                                : "Iniciado";

                Integer idRep = reporte != null ? reporte.getIdReporte()
                                : (orden.getReporte() != null ? orden.getReporte().getIdReporte() : null);
                LocalDateTime fechaIngreso = reporte != null ? reporte.getFechaIngreso()
                                : (orden.getReporte() != null ? orden.getReporte().getFechaIngreso() : null);

                // Buscar comentario de cuadrilla en TBLHISTORIAL con ID_ESTADO = 3
                String comentarioCuadrilla = null;
                if (idRep != null) {
                        List<HistorialReporte> hist = historialReporteRepository
                                        .findByReporteIdOrderByFechaCambioAsc(idRep);
                        comentarioCuadrilla = hist.stream()
                                        .filter(h -> h.getEstado() != null && (Integer.valueOf(3)
                                                        .equals(h.getEstado().getIdEstado())
                                                        || "EN REVISION".equalsIgnoreCase(h.getEstado().getNombre())
                                                        || "EN REVISIÓN".equalsIgnoreCase(h.getEstado().getNombre())
                                                        || "EN PROCESO".equalsIgnoreCase(h.getEstado().getNombre())))
                                        .map(HistorialReporte::getObservacion)
                                        .filter(obs -> obs != null && !obs.isBlank())
                                        .reduce((first, second) -> second)
                                        .orElse(null);
                }

                if (comentarioCuadrilla == null || comentarioCuadrilla.isBlank()) {
                        String cuad = orden.getCuadrilla() != null ? orden.getCuadrilla()
                                        : "la Cuadrilla de Operaciones";
                        String resp = orden.getResponsable() != null ? orden.getResponsable() : "el Encargado Técnico";
                        comentarioCuadrilla = "Trabajo ejecutado en terreno por " + cuad + " a cargo de " + resp
                                        + ". Se cumplieron las instrucciones operativas y la zona quedó habilitada.";
                }

                return OrdenTrabajoResponseDTO.builder()
                                .idOrden(orden.getIdOrden())
                                .idReporte(idRep)
                                .codigoReporte(formatearCodigo(idRep, fechaIngreso))
                                .cuadrilla(orden.getCuadrilla())
                                .responsable(orden.getResponsable())
                                .cantidadPersonal(orden.getCantidadPersonal() != null
                                                ? orden.getCantidadPersonal().intValue()
                                                : null)
                                .vehiculo(orden.getVehiculo())
                                .fechaVisita(orden.getFechaVisita())
                                .horaVisita(orden.getHoraVisita())
                                .duracionEstimada(orden.getDuracionEstimada())
                                .materiales(orden.getMateriales())
                                .instrucciones(orden.getInstrucciones())
                                .estadoReporte(estadoNombre)
                                .fechaCierreAdmin(orden.getFechaCierreAdmin())
                                .observacionCierreAdmin(orden.getObservacionCierreAdmin())
                                .comentarioCuadrilla(comentarioCuadrilla)
                                .build();
        }

        private PasoLineaTiempoDTO construirPaso(int numero, String titulo, int ordenActual, int ordenPaso,
                        List<HistorialReporte> historial, String defaultDesc) {
                String estado;
                String fecha = null;
                String descripcion = defaultDesc;

                // Buscar si existe un registro en el historial para este estado
                Optional<HistorialReporte> registroHistorial = historial.stream()
                                .filter(h -> h.getEstado() != null && h.getEstado().getOrden() == ordenPaso)
                                .findFirst();

                if (registroHistorial.isPresent()) {
                        fecha = registroHistorial.get().getFechaCambio().format(DATE_FORMATTER);
                        if (registroHistorial.get().getObservacion() != null
                                        && !registroHistorial.get().getObservacion().isBlank()) {
                                descripcion = registroHistorial.get().getObservacion();
                        }
                }

                if (ordenActual > ordenPaso) {
                        estado = "completado";
                } else if (ordenActual == ordenPaso) {
                        estado = "en-proceso";
                } else {
                        estado = "pendiente";
                        fecha = null;
                }

                return PasoLineaTiempoDTO.builder()
                                .numero(numero)
                                .titulo(titulo)
                                .fecha(fecha)
                                .estado(estado)
                                .descripcion(descripcion)
                                .build();
        }

        private ReporteDTO mapearAResumenDTO(Reporte reporte) {
                // IMPORTANTE: se mapea a DTO con tipos primitivos/String.
                // Nunca se expone la entidad JPA directamente para evitar ciclos JSON
                // y el HTTP 500 que esto genera en Swagger /v3/api-docs.
                return ReporteDTO.builder()
                                .id(reporte.getIdReporte()) // Integer — clave PK para rutas y operaciones
                                .codigo(formatearCodigo(reporte)) // String — código dinámico RPT-{AÑO}-{ID_FORMATEADO}
                                .categoria(reporte.getCategoria() != null
                                                ? formatearCapital(reporte.getCategoria().getNombre())
                                                : "Sin Categoría")
                                .texto(reporte.getTextoReporte())
                                .direccion(reporte.getDireccion())
                                .fechaIngreso(reporte.getFechaIngreso().format(DATE_FORMATTER))
                                .urgencia(reporte.getUrgenciaFinal() != null
                                                ? formatearCapital(reporte.getUrgenciaFinal().getNombre())
                                                : "Baja")
                                .urgenciaMl(reporte.getUrgenciaMl() != null
                                                ? formatearCapital(reporte.getUrgenciaMl().getNombre())
                                                : (reporte.getUrgenciaFinal() != null
                                                                ? formatearCapital(
                                                                                reporte.getUrgenciaFinal().getNombre())
                                                                : "Baja"))
                                .sentimiento(reporte.getSentimiento() != null
                                                ? formatearCapital(reporte.getSentimiento().getNombre())
                                                : "Neutral")
                                .estado(reporte.getEstado() != null ? formatearCapital(reporte.getEstado().getNombre())
                                                : "No Iniciado")
                                .idEstado(reporte.getEstado() != null ? reporte.getEstado().getIdEstado() : 1)
                                .nivelRiesgo(reporte.getNivelRiesgo() != null ? reporte.getNivelRiesgo().name()
                                                : "NINGUNO")
                                .huboAjuste(reporte.getHuboAjuste())
                                .build();
        }

        private Integer parsearIdentificador(String idStr) {
                try {
                        if (idStr.toUpperCase().startsWith("RPT-")) {
                                String[] partes = idStr.split("-");
                                return Integer.parseInt(partes[partes.length - 1]);
                        }
                        return Integer.parseInt(idStr);
                } catch (Exception e) {
                        return 1;
                }
        }

        /**
         * Construye dinámicamente el código de presentación del reporte con formato
         * RPT-{AÑO}-{ID_FORMATEADO}.
         * Si la fecha de ingreso es nula, utiliza como salvaguarda el año actual del
         * sistema.
         *
         * @param id           Identificador numérico del reporte
         * @param fechaIngreso Fecha y hora de ingreso del reporte
         * @return Código formateado (ej. RPT-2026-0005)
         */
        private String formatearCodigo(Integer id, LocalDateTime fechaIngreso) {
                int anio = (fechaIngreso != null) ? fechaIngreso.getYear() : LocalDate.now().getYear();
                return String.format("RPT-%d-%04d", anio, id != null ? id : 1);
        }

        /**
         * Sobrecarga conveniente para entidades Reporte.
         *
         * @param reporte Entidad Reporte
         * @return Código formateado (ej. RPT-2026-0005)
         */
        private String formatearCodigo(Reporte reporte) {
                if (reporte == null) {
                        return String.format("RPT-%d-%04d", LocalDate.now().getYear(), 1);
                }
                return formatearCodigo(reporte.getIdReporte(), reporte.getFechaIngreso());
        }

        /**
         * Sobrecarga de compatibilidad para identificadores numéricos.
         *
         * @param id Identificador numérico del reporte
         * @return Código formateado con año actual como salvaguarda (ej. RPT-2026-0005)
         */

        private String formatearCapital(String texto) {
                if (texto == null || texto.isEmpty())
                        return "";
                String lower = texto.toLowerCase();
                return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        }

        private Sentimiento obtenerSentimientoDefault() {
                return sentimientoRepository.findByNombreIgnoreCase("NEUTRAL")
                                .orElseGet(() -> sentimientoRepository.findById(2)
                                                .orElseGet(() -> sentimientoRepository.findAll().stream().findFirst()
                                                                .orElseThrow(() -> new ResponseStatusException(
                                                                                HttpStatus.INTERNAL_SERVER_ERROR,
                                                                                "No hay sentimientos configurados"))));
        }

        private Urgencia obtenerUrgenciaDefault() {
                return urgenciaRepository.findByNombreIgnoreCase("BAJA")
                                .orElseGet(() -> urgenciaRepository.findById(1)
                                                .orElseGet(() -> urgenciaRepository.findAll().stream().findFirst()
                                                                .orElseThrow(() -> new ResponseStatusException(
                                                                                HttpStatus.INTERNAL_SERVER_ERROR,
                                                                                "No hay urgencias configuradas"))));
        }
}
