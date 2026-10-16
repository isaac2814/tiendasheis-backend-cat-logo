package com.sheis.catalogo.repository;

import com.sheis.catalogo.domain.MetodoPagoImagen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MetodoPagoImagenRepository extends JpaRepository<MetodoPagoImagen, String> {

    @Query("SELECT img FROM MetodoPagoImagen img JOIN FETCH img.metodoPago m WHERE m.id = :id")
    Optional<MetodoPagoImagen> findByMetodoPagoId(@Param("id") String id);

    @Query("SELECT m.id, img.actualizadoEn FROM MetodoPagoImagen img JOIN img.metodoPago m")
    List<Object[]> findMetadatosVersiones();

    @Query("SELECT COUNT(img) > 0 FROM MetodoPagoImagen img JOIN img.metodoPago m WHERE m.id = :id")
    boolean existsByMetodoPagoId(@Param("id") String id);
}
