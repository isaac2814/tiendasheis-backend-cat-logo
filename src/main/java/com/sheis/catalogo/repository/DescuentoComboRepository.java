package com.sheis.catalogo.repository;

import com.sheis.catalogo.domain.DescuentoCombo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DescuentoComboRepository extends JpaRepository<DescuentoCombo, Integer> {

    List<DescuentoCombo> findByActivoTrueOrderByCantidadServiciosAsc();

    List<DescuentoCombo> findAllByOrderByCantidadServiciosAsc();
}
