package com.sheis.catalogo.repository;

import com.sheis.catalogo.domain.ProductoVariante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoVarianteRepository extends JpaRepository<ProductoVariante, Long> {

    List<ProductoVariante> findByProductoIdOrderByOrdenAscIdAsc(Long productoId);

    List<ProductoVariante> findByProductoIdAndActivaTrueOrderByOrdenAscIdAsc(Long productoId);

    List<ProductoVariante> findByActivaTrueOrderByOrdenAscIdAsc();

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    boolean existsByProductoIdAndNombreIgnoreCase(Long productoId, String nombre);

    boolean existsByProductoIdAndNombreIgnoreCaseAndIdNot(Long productoId, String nombre, Long id);
}
