package com.sheis.catalogo.repository;

import com.sheis.catalogo.domain.ServicioImagen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServicioImagenRepository extends JpaRepository<ServicioImagen, Long> {

    @Query("SELECT img FROM ServicioImagen img JOIN FETCH img.skuCategoria s WHERE s.codigo = :codigo")
    Optional<ServicioImagen> findByCodigoServicio(@Param("codigo") String codigo);

    @Query("SELECT s.codigo, img.actualizadoEn FROM ServicioImagen img JOIN img.skuCategoria s")
    List<Object[]> findMetadatosVersiones();

    @Query("SELECT COUNT(img) > 0 FROM ServicioImagen img JOIN img.skuCategoria s WHERE s.codigo = :codigo")
    boolean existsByCodigoServicio(@Param("codigo") String codigo);
}
