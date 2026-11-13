package com.sheis.catalogo.repository;

import com.sheis.catalogo.domain.ProductoVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductoVideoRepository extends JpaRepository<ProductoVideo, Long> {

    Optional<ProductoVideo> findByProductoId(Long productoId);

    void deleteByProductoId(Long productoId);

    List<ProductoVideo> findByProductoIdIn(Collection<Long> productoIds);
}
