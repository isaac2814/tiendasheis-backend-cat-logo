package com.sheis.catalogo.repository;

import com.sheis.catalogo.domain.Producto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findAllByOrderByOrdenAscIdAsc();

    List<Producto> findByVisibleTrueOrderByOrdenAscIdAsc();

    long countByCategoriaId(Long categoriaId);

    List<Producto> findByCategoriaIdOrderByOrdenAscIdAsc(Long categoriaId);

    List<Producto> findByCategoriaIdAndVisibleTrueOrderByOrdenAscIdAsc(Long categoriaId);
}
