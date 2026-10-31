package com.sheis.catalogo.repository;

import com.sheis.catalogo.domain.ProductoImagen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoImagenRepository extends JpaRepository<ProductoImagen, Long> {

    List<ProductoImagen> findByProductoIdOrderByOrdenAscIdAsc(Long productoId);

    Optional<ProductoImagen> findByIdAndProductoId(Long id, Long productoId);

    int countByProductoId(Long productoId);

    @Query("SELECT MAX(pi.orden) FROM ProductoImagen pi WHERE pi.producto.id = :productoId")
    Optional<Integer> findMaxOrdenByProductoId(Long productoId);
}
