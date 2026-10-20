package com.gestionreportes.dashboard.repository;

import com.gestionreportes.dashboard.model.HistorialReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialReporteRepository extends JpaRepository<HistorialReporte, Integer> {

    @Query("SELECT h FROM HistorialReporte h " +
           "JOIN FETCH h.estado " +
           "WHERE h.reporte.idReporte = :idReporte " +
           "ORDER BY h.fechaCambio ASC")
    List<HistorialReporte> findByReporteIdOrderByFechaCambioAsc(@Param("idReporte") Integer idReporte);
}
