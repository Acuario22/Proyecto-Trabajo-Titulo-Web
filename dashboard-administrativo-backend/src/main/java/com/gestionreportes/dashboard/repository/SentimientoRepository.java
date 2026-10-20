package com.gestionreportes.dashboard.repository;

import com.gestionreportes.dashboard.model.Sentimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SentimientoRepository extends JpaRepository<Sentimiento, Integer> {
    Optional<Sentimiento> findByNombreIgnoreCase(String nombre);
}
