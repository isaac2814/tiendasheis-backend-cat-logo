package com.sheis.catalogo.repository;

import com.sheis.catalogo.domain.Recomendacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecomendacionRepository extends JpaRepository<Recomendacion, Long> {

    List<Recomendacion> findAllByOrderByOrdenAscIdAsc();

    Optional<Recomendacion> findTopByOrderByOrdenDesc();

    @Query("""
        SELECT r FROM Recomendacion r
        LEFT JOIN r.producto p
        WHERE r.activa = true
          AND (:categoriaSlug IS NULL OR r.categoriaSlug = :categoriaSlug)
          AND (:productoId IS NULL OR (p IS NOT NULL AND p.id = :productoId))
        ORDER BY r.orden ASC, r.id ASC
    """)
    Page<Recomendacion> findPublicas(
            @Param("categoriaSlug") String categoriaSlug,
            @Param("productoId") Long productoId,
            Pageable pageable
    );
}
