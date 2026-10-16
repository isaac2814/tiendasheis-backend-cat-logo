package com.sheis.catalogo.repository;

import com.sheis.catalogo.domain.Combo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComboRepository extends JpaRepository<Combo, Long> {

    @Query("SELECT DISTINCT c FROM Combo c LEFT JOIN FETCH c.servicios s WHERE c.activo = true ORDER BY c.orden ASC")
    List<Combo> findActiveWithServicios();

    @Query("SELECT DISTINCT c FROM Combo c LEFT JOIN FETCH c.servicios s ORDER BY c.orden ASC")
    List<Combo> findAllWithServicios();
}
