package com.gestionreportes.dashboard.repository;

import com.gestionreportes.dashboard.model.Urgencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UrgenciaRepository extends JpaRepository<Urgencia, Integer> {
    Optional<Urgencia> findByNombreIgnoreCase(String nombre);
}
