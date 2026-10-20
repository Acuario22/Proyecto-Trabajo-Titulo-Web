package com.gestionreportes.dashboard.service.impl;

import com.gestionreportes.dashboard.dto.request.CerrarReporteRequestDTO;
import com.gestionreportes.dashboard.dto.request.CrearOrdenTrabajoRequestDTO;
import com.gestionreportes.dashboard.dto.request.CrearReporteRequestDTO;
import com.gestionreportes.dashboard.dto.response.*;
import com.gestionreportes.dashboard.model.*;
import com.gestionreportes.dashboard.repository.*;
import com.gestionreportes.dashboard.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public List<ReporteDTO> listarReportes(String categoria) {
        List<Reporte> reportes;
        if (categoria != null && !categoria.trim().isEmpty() && !categoria.equalsIgnoreCase("TODOS")) {
            reportes = reporteRepository.findAllWithRelationsByCategoria(categoria.trim());
        } else {
            reportes = reporteRepository.findAllWithRelations();
        }

        return reportes.stream()
                .map(this::mapearAResumenDTO)
                .collect(Collectors.toList());
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
                        .conteo(reporteRepository.countByCategoriaNombreIgnoreCase(cat.getNombre()))
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

        List<HistorialReporte> historial = historialReporteRepository.findByReporteIdOrderByFechaCambioAsc(idReporte);

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
        String estadoActualNombre = reporte.getEstado() != null ? formatearCapital(reporte.getEstado().getNombre())
                : "No Iniciado";

        // Obtener última observación del historial o descripción por defecto
        String detalleActual = historial.isEmpty() ? "En espera de asignación de cuadrilla."
                : historial.get(historial.size() - 1).getObservacion();
        if (detalleActual == null || detalleActual.isBlank()) {
            detalleActual = "Gestión en curso según el protocolo operativo.";
        }

        return TrazabilidadReporteDTO.builder()
                .codigo(formatearCodigo(reporte.getIdReporte()))
                .titulo("Reporte de " + categoriaFormateada)
                .descripcion(reporte.getTextoReporte())
                .direccion(reporte.getDireccion())
                .fechaIngreso(reporte.getFechaIngreso().format(DATE_FORMATTER))
                .urgencia(reporte.getUrgenciaFinal() != null ? formatearCapital(reporte.getUrgenciaFinal().getNombre())
                        : "Baja")
                .sentimiento(reporte.getSentimiento() != null ? formatearCapital(reporte.getSentimiento().getNombre())
                        : "Neutral")
                .estadoActual(estadoActualNombre)
                .detalleEstadoActual(detalleActual)
                .pasos(pasos)
                .build();
    }

    @Override
    @Transactional
    public ReporteDTO crearReporte(CrearReporteRequestDTO request) {
        Categoria categoria = categoriaRepository.findById(request.getIdCategoria())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoría no válida"));

        Sentimiento sentimiento = sentimientoRepository.findById(request.getIdSentimiento())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sentimiento no válido"));

        Urgencia urgenciaMl = urgenciaRepository.findById(request.getIdUrgenciaMl())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Urgencia ML no válida"));

        Urgencia urgenciaFinal = urgenciaRepository.findById(request.getIdUrgenciaFinal())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Urgencia final no válida"));

        EstadoReporte estadoInicial = estadoReporteRepository.findById(1)
                .orElseGet(() -> estadoReporteRepository.findAllByOrderByOrdenAsc().stream().findFirst()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "No hay estados configurados")));

        NivelRiesgo nivelRiesgo = NivelRiesgo.NINGUNO;
        try {
            if (request.getNivelRiesgo() != null) {
                nivelRiesgo = NivelRiesgo.valueOf(request.getNivelRiesgo().toUpperCase());
            }
        } catch (IllegalArgumentException ignored) {
        }

        Reporte nuevoReporte = Reporte.builder()
                .fechaIngreso(LocalDateTime.now())
                .textoReporte(request.getTextoReporte())
                .direccion(request.getDireccion())
                .categoria(categoria)
                .sentimiento(sentimiento)
                .urgenciaMl(urgenciaMl)
                .urgenciaFinal(urgenciaFinal)
                .nivelRiesgo(nivelRiesgo)
                .huboAjuste(Boolean.TRUE.equals(request.getHuboAjuste()))
                .estado(estadoInicial)
                .build();

        Reporte guardado = reporteRepository.save(nuevoReporte);

        // Registro inicial en TBLHISTORIAL
        HistorialReporte historialInicial = HistorialReporte.builder()
                .reporte(guardado)
                .estado(estadoInicial)
                .fechaCambio(LocalDateTime.now())
                .observacion("Reporte recibido y registrado en el sistema.")
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
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "Estado 'INICIADO' no encontrado en el sistema")));

        reporte.setEstado(estadoIniciado);
        reporteRepository.save(reporte);

        // Insertar registro en TBLHISTORIAL
        HistorialReporte nuevoHistorial = HistorialReporte.builder()
                .reporte(reporte)
                .estado(estadoIniciado)
                .fechaCambio(LocalDateTime.now())
                .observacion("Orden de trabajo asignada a " + request.getCuadrilla() + " - Responsable: "
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
        List<HistorialReporte> historial = historialReporteRepository.findByReporteIdOrderByFechaCambioAsc(idReporte);
        boolean tieneEstadoRevision = historial.stream()
                .anyMatch(h -> h.getEstado() != null && (Integer.valueOf(3).equals(h.getEstado().getIdEstado())
                        || "EN REVISION".equalsIgnoreCase(h.getEstado().getNombre())
                        || "EN REVISIÓN".equalsIgnoreCase(h.getEstado().getNombre())));

        if (!tieneEstadoRevision) {
            EstadoReporte estadoRevision = estadoReporteRepository.findById(3)
                    .orElseGet(() -> estadoReporteRepository.findByNombreIgnoreCase("EN REVISION")
                            .orElseGet(() -> estadoReporteRepository.findByNombreIgnoreCase("EN REVISIÓN")
                                    .orElseGet(() -> estadoReporteRepository.findByNombreIgnoreCase("EN PROCESO")
                                            .orElse(null))));

            if (estadoRevision != null) {
                String cuad = guardada.getCuadrilla() != null ? guardada.getCuadrilla() : "Cuadrilla asignada";
                String resp = guardada.getResponsable() != null ? guardada.getResponsable() : "Responsable";
                String comentarioCuadrilla = "Trabajo ejecutado en terreno por " + cuad + " a cargo de " + resp
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
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "Estado 'FINALIZADO' no encontrado en el sistema")));

        reporte.setEstado(estadoFinalizado);
        reporteRepository.save(reporte);

        // e) Insertar el registro final en TBLHISTORIAL
        HistorialReporte hFinal = HistorialReporte.builder()
                .reporte(reporte)
                .estado(estadoFinalizado)
                .fechaCambio(LocalDateTime.now())
                .observacion("Cierre aprobado por administración: " + request.getObservacionCierreAdmin())
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

        // Buscar comentario de cuadrilla en TBLHISTORIAL con ID_ESTADO = 3
        String comentarioCuadrilla = null;
        if (idRep != null) {
            List<HistorialReporte> hist = historialReporteRepository.findByReporteIdOrderByFechaCambioAsc(idRep);
            comentarioCuadrilla = hist.stream()
                    .filter(h -> h.getEstado() != null && (Integer.valueOf(3).equals(h.getEstado().getIdEstado())
                            || "EN REVISION".equalsIgnoreCase(h.getEstado().getNombre())
                            || "EN REVISIÓN".equalsIgnoreCase(h.getEstado().getNombre())
                            || "EN PROCESO".equalsIgnoreCase(h.getEstado().getNombre())))
                    .map(HistorialReporte::getObservacion)
                    .filter(obs -> obs != null && !obs.isBlank())
                    .reduce((first, second) -> second)
                    .orElse(null);
        }

        if (comentarioCuadrilla == null || comentarioCuadrilla.isBlank()) {
            String cuad = orden.getCuadrilla() != null ? orden.getCuadrilla() : "la Cuadrilla de Operaciones";
            String resp = orden.getResponsable() != null ? orden.getResponsable() : "el Encargado Técnico";
            comentarioCuadrilla = "Trabajo ejecutado en terreno por " + cuad + " a cargo de " + resp
                    + ". Se cumplieron las instrucciones operativas y la zona quedó habilitada.";
        }

        return OrdenTrabajoResponseDTO.builder()
                .idOrden(orden.getIdOrden())
                .idReporte(idRep)
                .codigoReporte(formatearCodigo(idRep))
                .cuadrilla(orden.getCuadrilla())
                .responsable(orden.getResponsable())
                .cantidadPersonal(orden.getCantidadPersonal() != null ? orden.getCantidadPersonal().intValue() : null)
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
                .codigo(formatearCodigo(reporte.getIdReporte())) // String — código de visualización RPT-2024-XXXX
                .categoria(reporte.getCategoria() != null ? formatearCapital(reporte.getCategoria().getNombre())
                        : "Sin Categoría")
                .texto(reporte.getTextoReporte())
                .direccion(reporte.getDireccion())
                .fechaIngreso(reporte.getFechaIngreso().format(DATE_FORMATTER))
                .urgencia(reporte.getUrgenciaFinal() != null ? formatearCapital(reporte.getUrgenciaFinal().getNombre())
                        : "Baja")
                .sentimiento(reporte.getSentimiento() != null ? formatearCapital(reporte.getSentimiento().getNombre())
                        : "Neutral")
                .estado(reporte.getEstado() != null ? formatearCapital(reporte.getEstado().getNombre()) : "No Iniciado")
                .nivelRiesgo(reporte.getNivelRiesgo() != null ? reporte.getNivelRiesgo().name() : "NINGUNO")
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

    private String formatearCodigo(Integer id) {
        return String.format("RPT-2024-%04d", id != null ? id : 1);
    }

    private String formatearCapital(String texto) {
        if (texto == null || texto.isEmpty())
            return "";
        String lower = texto.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
