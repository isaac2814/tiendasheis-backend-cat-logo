package com.sheis.catalogo.repository;

import com.sheis.catalogo.domain.CategoriaProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaProductoRepository extends JpaRepository<CategoriaProducto, Long> {

    Optional<CategoriaProducto> findByNombreIgnoreCase(String nombre);

    List<CategoriaProducto> findAllByOrderByOrdenAscIdAsc();

    List<CategoriaProducto> findByVisibleTrueOrderByOrdenAscIdAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
