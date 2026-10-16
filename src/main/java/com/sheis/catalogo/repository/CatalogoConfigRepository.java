package com.sheis.catalogo.repository;

import com.sheis.catalogo.domain.CatalogoConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CatalogoConfigRepository extends JpaRepository<CatalogoConfig, String> {

    Optional<CatalogoConfig> findByClave(String clave);
}
