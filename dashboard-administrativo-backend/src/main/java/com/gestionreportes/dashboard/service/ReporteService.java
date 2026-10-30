package com.gestionreportes.dashboard.service;

import com.gestionreportes.dashboard.dto.request.CerrarReporteRequestDTO;
import com.gestionreportes.dashboard.dto.request.CrearOrdenTrabajoRequestDTO;
import com.gestionreportes.dashboard.dto.request.CrearReporteRequestDTO;
import com.gestionreportes.dashboard.dto.response.*;

import java.util.List;

public interface ReporteService {

    /** Retorna todos los reportes, o filtrados por categoría si se proporciona. */
    List<ReporteDTO> listarReportes(String categoria);

    /** Retorna reportes filtrados por categoría y/o ID de estado si se proporcionan. */
    List<ReporteDTO> listarReportes(String categoria, Integer idEstado);

    /** Retorna reportes filtrados únicamente por ID de estado (1 a 4). */
    List<ReporteDTO> listarPorEstado(Integer idEstado);

    /** Consulta unitaria optimizada por PK (JOIN FETCH, sin N+1). */
    ReporteDTO obtenerReportePorId(Integer id);

    List<CategoriaConteoDTO> listarCategoriasConConteo();

    DashboardMetricasDTO obtenerMetricas();

    TrazabilidadReporteDTO obtenerTrazabilidad(String identificador);

    ReporteDTO crearReporte(CrearReporteRequestDTO request);

    /** Crea o actualiza la orden de trabajo para un reporte y avanza su estado a INICIADO (id 2). */
    OrdenTrabajoResponseDTO crearOrdenTrabajo(Integer idReporte, CrearOrdenTrabajoRequestDTO request);

    /** Recupera la orden de trabajo asignada a un reporte, o lanza 404 si no tiene. */
    OrdenTrabajoResponseDTO obtenerOrdenTrabajoPorReporte(Integer idReporte);

    /** Valida el trabajo de la cuadrilla, registra la observación administrativa de cierre y pasa a estado FINALIZADO (id 4). */
    OrdenTrabajoResponseDTO finalizarReporte(Integer idReporte, CerrarReporteRequestDTO request);
}
