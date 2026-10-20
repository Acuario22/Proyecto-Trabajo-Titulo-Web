package com.gestionreportes.dashboard.repository;

import com.gestionreportes.dashboard.model.OrdenTrabajo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, Integer> {

    Optional<OrdenTrabajo> findByReporte_IdReporte(Integer idReporte);

    @Query("SELECT o FROM OrdenTrabajo o JOIN FETCH o.reporte r JOIN FETCH r.estado WHERE r.idReporte = :idReporte")
    Optional<OrdenTrabajo> findByReporteIdWithRelations(@Param("idReporte") Integer idReporte);
}
