package com.sheis.catalogo.repository;

import com.sheis.catalogo.domain.AdminUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminUsuarioRepository extends JpaRepository<AdminUsuario, Long> {
    Optional<AdminUsuario> findByEmailIgnoreCaseAndActivoTrue(String email);
    boolean existsByEmailIgnoreCase(String email);
}
