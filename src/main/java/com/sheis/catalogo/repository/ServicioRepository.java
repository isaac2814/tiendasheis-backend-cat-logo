package com.sheis.catalogo.repository;

import com.sheis.catalogo.domain.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Long> {
    Optional<Servicio> findByNombre(String nombre);
}
