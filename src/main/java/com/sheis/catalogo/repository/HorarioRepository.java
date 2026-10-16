package com.sheis.catalogo.repository;

import com.sheis.catalogo.domain.Horario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HorarioRepository extends JpaRepository<Horario, Integer> {

    List<Horario> findAllByOrderByOrdenAsc();
}
