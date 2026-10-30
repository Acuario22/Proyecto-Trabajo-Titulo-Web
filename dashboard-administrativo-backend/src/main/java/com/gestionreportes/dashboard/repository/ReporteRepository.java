package com.gestionreportes.dashboard.repository;

import com.gestionreportes.dashboard.model.Reporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReporteRepository extends JpaRepository<Reporte, Integer> {

    List<Reporte> findAllByOrderByFechaIngresoDesc();

    List<Reporte> findByCategoriaNombreIgnoreCaseOrderByFechaIngresoDesc(String nombreCategoria);

    long countByCategoriaNombreIgnoreCase(String nombreCategoria);

    long countByUrgenciaFinalNombreIgnoreCase(String nombreUrgencia);

    long countByEstadoNombreIgnoreCase(String nombreEstado);

    @Query("SELECT r FROM Reporte r " +
           "JOIN FETCH r.categoria " +
           "JOIN FETCH r.sentimiento " +
           "JOIN FETCH r.urgenciaMl " +
           "JOIN FETCH r.urgenciaFinal " +
           "JOIN FETCH r.estado " +
           "ORDER BY r.fechaIngreso DESC")
    List<Reporte> findAllWithRelations();

    @Query("SELECT r FROM Reporte r " +
           "JOIN FETCH r.categoria " +
           "JOIN FETCH r.sentimiento " +
           "JOIN FETCH r.urgenciaMl " +
           "JOIN FETCH r.urgenciaFinal " +
           "JOIN FETCH r.estado " +
           "WHERE UPPER(r.categoria.nombre) = UPPER(:categoria) " +
           "ORDER BY r.fechaIngreso DESC")
    List<Reporte> findAllWithRelationsByCategoria(@Param("categoria") String categoria);

    @Query("SELECT r FROM Reporte r " +
           "JOIN FETCH r.categoria " +
           "JOIN FETCH r.sentimiento " +
           "JOIN FETCH r.urgenciaMl " +
           "JOIN FETCH r.urgenciaFinal " +
           "JOIN FETCH r.estado " +
           "WHERE r.estado.idEstado = :idEstado " +
           "ORDER BY r.fechaIngreso DESC")
    List<Reporte> findAllWithRelationsByEstado(@Param("idEstado") Integer idEstado);

    @Query("SELECT r FROM Reporte r " +
           "JOIN FETCH r.categoria " +
           "JOIN FETCH r.sentimiento " +
           "JOIN FETCH r.urgenciaMl " +
           "JOIN FETCH r.urgenciaFinal " +
           "JOIN FETCH r.estado " +
           "WHERE UPPER(r.categoria.nombre) = UPPER(:categoria) " +
           "AND r.estado.idEstado = :idEstado " +
           "ORDER BY r.fechaIngreso DESC")
    List<Reporte> findAllWithRelationsByCategoriaAndEstado(@Param("categoria") String categoria, @Param("idEstado") Integer idEstado);

    /**
     * Búsqueda unitaria por PK con JOIN FETCH para evitar el problema N+1.
     * Optimiza GET /api/reportes/{id} para la navegación interna del panel.
     */
    @Query("SELECT r FROM Reporte r " +
           "JOIN FETCH r.categoria " +
           "JOIN FETCH r.sentimiento " +
           "JOIN FETCH r.urgenciaMl " +
           "JOIN FETCH r.urgenciaFinal " +
           "JOIN FETCH r.estado " +
           "WHERE r.idReporte = :id")
    Optional<Reporte> findByIdWithRelations(@Param("id") Integer id);
}
