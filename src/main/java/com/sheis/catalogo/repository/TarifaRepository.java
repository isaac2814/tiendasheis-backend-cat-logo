package com.sheis.catalogo.repository;

import com.sheis.catalogo.domain.Tarifa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TarifaRepository extends JpaRepository<Tarifa, Long> {

    List<Tarifa> findBySkuCategoriaCodigoOrderByDiasAscCantidadPerfilesAsc(String codigo);

    List<Tarifa> findBySkuCategoriaCodigoAndActivoTrueOrderByDiasAscCantidadPerfilesAsc(String codigo);

    @Modifying
    @Query("DELETE FROM Tarifa t WHERE t.skuCategoria.codigo = :codigo")
    void deleteBySkuCategoriaCodigo(String codigo);
}
