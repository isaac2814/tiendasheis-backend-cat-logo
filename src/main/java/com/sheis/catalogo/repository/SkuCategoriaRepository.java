package com.sheis.catalogo.repository;

import com.sheis.catalogo.domain.SkuCategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkuCategoriaRepository extends JpaRepository<SkuCategoria, Long> {

    Optional<SkuCategoria> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    @Query("SELECT DISTINCT s FROM SkuCategoria s LEFT JOIN FETCH s.tarifas t WHERE s.visible = true ORDER BY s.orden ASC")
    List<SkuCategoria> findVisibleWithTarifas();

    @Query("SELECT DISTINCT s FROM SkuCategoria s LEFT JOIN FETCH s.tarifas t ORDER BY s.orden ASC")
    List<SkuCategoria> findAllWithTarifas();

    List<SkuCategoria> findByCodigoIn(List<String> codigos);

    List<SkuCategoria> findByFavoritoOrdenIsNotNullOrderByFavoritoOrdenAsc();

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE SkuCategoria s SET s.favoritoOrden = null")
    void clearAllFavoritoOrden();
}
