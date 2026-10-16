package com.sheis.catalogo.repository;

import com.sheis.catalogo.domain.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetodoPagoRepository extends JpaRepository<MetodoPago, String> {

    List<MetodoPago> findByActivoTrueOrderByOrdenAsc();

    List<MetodoPago> findAllByOrderByOrdenAsc();
}
