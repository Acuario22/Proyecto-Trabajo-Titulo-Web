package com.gestionreportes.dashboard.repository;

import com.gestionreportes.dashboard.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {
    Optional<Categoria> findByNombreIgnoreCase(String nombre);
}
