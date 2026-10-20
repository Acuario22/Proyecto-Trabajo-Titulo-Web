package com.gestionreportes.dashboard.repository;

import com.gestionreportes.dashboard.model.EstadoReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstadoReporteRepository extends JpaRepository<EstadoReporte, Integer> {
    Optional<EstadoReporte> findByNombreIgnoreCase(String nombre);
    List<EstadoReporte> findAllByOrderByOrdenAsc();
}
