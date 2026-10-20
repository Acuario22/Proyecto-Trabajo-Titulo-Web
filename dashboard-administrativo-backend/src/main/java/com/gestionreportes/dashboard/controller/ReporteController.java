package com.gestionreportes.dashboard.controller;

import com.gestionreportes.dashboard.dto.request.CerrarReporteRequestDTO;
import com.gestionreportes.dashboard.dto.request.CrearOrdenTrabajoRequestDTO;
import com.gestionreportes.dashboard.dto.request.CrearReporteRequestDTO;
import com.gestionreportes.dashboard.dto.response.CategoriaConteoDTO;
import com.gestionreportes.dashboard.dto.response.DashboardMetricasDTO;
import com.gestionreportes.dashboard.dto.response.OrdenTrabajoResponseDTO;
import com.gestionreportes.dashboard.dto.response.ReporteDTO;
import com.gestionreportes.dashboard.dto.response.TrazabilidadReporteDTO;
import com.gestionreportes.dashboard.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Reportes", description = "Endpoints para la gestión y consulta de reportes ciudadanos de infraestructura urbana")
public class ReporteController {

    private final ReporteService reporteService;

    /**
     * GET /api/reportes/
     * Retorna todos los reportes, con filtro opcional por categoría.
     * Ejemplo sin filtro: GET /api/reportes
     * Ejemplo con filtro: GET /api/reportes?categoria=Pavimentacion
     */
    @GetMapping
    @Operation(summary = "Listar reportes", description = "Retorna todos los reportes de la base de datos. Si se proporciona el parámetro 'categoria', filtra por ese valor (búsqueda insensible a mayúsculas).")
    public ResponseEntity<List<ReporteDTO>> listarReportes(
            @Parameter(description = "Nombre de la categoría para filtrar (opcional)", example = "Pavimentacion") @RequestParam(required = false) String categoria) {
        return ResponseEntity.ok(reporteService.listarReportes(categoria));
    }

    /**
     * GET /api/reportes/{id}
     * Consulta unitaria por PK (Integer). Usa JOIN FETCH para evitar N+1.
     * Ejemplo: GET /api/reportes/1
     */
    @GetMapping("/{id:[0-9]+}")
    @Operation(summary = "Obtener reporte por ID", description = "Consulta un reporte específico por su clave primaria (ID numérico). Optimizado con JOIN FETCH para rendimiento.")
    public ResponseEntity<ReporteDTO> obtenerReportePorId(
            @Parameter(description = "ID numérico (PK) del reporte", example = "1") @PathVariable Integer id) {
        return ResponseEntity.ok(reporteService.obtenerReportePorId(id));
    }

    /**
     * GET /api/reportes/todos
     * Alias explícito que retorna siempre todos los reportes sin filtros.
     * Ejemplo: GET /api/reportes/todos
     */
    @GetMapping("/todos")
    @Operation(summary = "Listar todos los reportes", description = "Alias explícito que retorna la lista completa de todos los reportes sin parámetros.")
    public ResponseEntity<List<ReporteDTO>> listarTodosLosReportes() {
        return ResponseEntity.ok(reporteService.listarReportes(null));
    }

    /**
     * GET /api/reportes/categorias
     * Retorna las categorías activas con el conteo de reportes asociados.
     */
    @GetMapping("/categorias")
    @Operation(summary = "Listar categorías con conteo", description = "Obtiene las categorías activas en la base de datos junto con la cantidad de reportes asociados a cada una.")
    public ResponseEntity<List<CategoriaConteoDTO>> listarCategorias() {
        return ResponseEntity.ok(reporteService.listarCategoriasConConteo());
    }

    /**
     * GET /api/reportes/metricas
     * Retorna los KPIs para las tarjetas superiores del dashboard.
     */
    @GetMapping("/metricas")
    @Operation(summary = "Obtener métricas KPI", description = "Retorna los totales y conteos para las tarjetas de resumen del panel administrativo (total reportes, urgencia alta, finalizados).")
    public ResponseEntity<DashboardMetricasDTO> obtenerMetricas() {
        return ResponseEntity.ok(reporteService.obtenerMetricas());
    }

    /**
     * GET /api/reportes/{identificador}/trazabilidad
     * Acepta ID numérico o código RPT-2024-XXXX.
     */
    @GetMapping("/{identificador}/trazabilidad")
    @Operation(summary = "Obtener trazabilidad de reporte", description = "Retorna el detalle completo y la línea de tiempo de seguimiento de un reporte. Acepta ID numérico (ej: 1) o código formateado (ej: RPT-2024-0001).")
    public ResponseEntity<TrazabilidadReporteDTO> obtenerTrazabilidad(
            @Parameter(description = "ID numérico o código RPT-2024-XXXX del reporte", example = "RPT-2024-0001") @PathVariable String identificador) {
        return ResponseEntity.ok(reporteService.obtenerTrazabilidad(identificador));
    }

    /**
     * POST /api/reportes
     * Registra un nuevo reporte ciudadano.
     */
    @PostMapping
    @Operation(summary = "Crear nuevo reporte", description = "Registra un nuevo reporte ciudadano en el sistema con todos los atributos requeridos.")
    public ResponseEntity<ReporteDTO> crearReporte(
            @Valid @RequestBody CrearReporteRequestDTO request) {
        ReporteDTO creado = reporteService.crearReporte(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /**
     * POST /api/reportes/{id}/orden-trabajo
     * Registra o actualiza la orden de trabajo para el reporte y cambia su estado a INICIADO.
     */
    @PostMapping("/{id:[0-9]+}/orden-trabajo")
    @Operation(summary = "Crear orden de trabajo", description = "Asigna una cuadrilla y programa la atención de un reporte ciudadano, avanzando su estado a INICIADO e ingresando registro en el historial.")
    public ResponseEntity<OrdenTrabajoResponseDTO> crearOrdenTrabajo(
            @Parameter(description = "ID numérico (PK) del reporte", example = "1") @PathVariable Integer id,
            @Valid @RequestBody CrearOrdenTrabajoRequestDTO request) {
        OrdenTrabajoResponseDTO orden = reporteService.crearOrdenTrabajo(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(orden);
    }

    /**
     * GET /api/reportes/{id}/orden-trabajo
     * Obtiene la orden de trabajo asignada previamente a un reporte.
     */
    @GetMapping("/{id:[0-9]+}/orden-trabajo")
    @Operation(summary = "Obtener orden de trabajo", description = "Consulta los detalles de la orden de trabajo vinculada a un reporte.")
    public ResponseEntity<OrdenTrabajoResponseDTO> obtenerOrdenTrabajo(
            @Parameter(description = "ID numérico (PK) del reporte", example = "1") @PathVariable Integer id) {
        return ResponseEntity.ok(reporteService.obtenerOrdenTrabajoPorReporte(id));
    }

    /**
     * POST /api/reportes/{id}/finalizar
     * Valida el trabajo de la cuadrilla, registra la observación administrativa de cierre
     * y avanza el reporte al estado FINALIZADO (id 4).
     */
    @PostMapping("/{id:[0-9]+}/finalizar")
    @Operation(summary = "Finalizar reporte y aprobar cierre", description = "Valida el trabajo de la cuadrilla, registra la observación administrativa de cierre en TBLORDENTRABAJO y avanza el reporte a estado FINALIZADO con registro en el historial.")
    public ResponseEntity<OrdenTrabajoResponseDTO> finalizarReporte(
            @Parameter(description = "ID numérico (PK) del reporte", example = "1") @PathVariable Integer id,
            @Valid @RequestBody CerrarReporteRequestDTO request) {
        OrdenTrabajoResponseDTO orden = reporteService.finalizarReporte(id, request);
        return ResponseEntity.ok(orden);
    }
}
