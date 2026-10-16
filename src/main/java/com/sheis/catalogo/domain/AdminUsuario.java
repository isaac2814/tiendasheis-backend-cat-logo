package com.sheis.catalogo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "admin_usuarios")
@Getter
@Setter
public class AdminUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    private String nombre;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en")
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    public AdminUsuario() {
    }

    public AdminUsuario(Long id, String email, String passwordHash, String nombre, Boolean activo, OffsetDateTime creadoEn) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nombre = nombre;
        this.activo = activo != null ? activo : true;
        this.creadoEn = creadoEn != null ? creadoEn : OffsetDateTime.now();
    }

    public static AdminUsuarioBuilder builder() {
        return new AdminUsuarioBuilder();
    }

    public static class AdminUsuarioBuilder {
        private Long id;
        private String email;
        private String passwordHash;
        private String nombre;
        private Boolean activo = true;
        private OffsetDateTime creadoEn = OffsetDateTime.now();

        public AdminUsuarioBuilder id(Long id) { this.id = id; return this; }
        public AdminUsuarioBuilder email(String email) { this.email = email; return this; }
        public AdminUsuarioBuilder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public AdminUsuarioBuilder nombre(String nombre) { this.nombre = nombre; return this; }
        public AdminUsuarioBuilder activo(Boolean activo) { this.activo = activo; return this; }
        public AdminUsuarioBuilder creadoEn(OffsetDateTime creadoEn) { this.creadoEn = creadoEn; return this; }

        public AdminUsuario build() {
            return new AdminUsuario(id, email, passwordHash, nombre, activo, creadoEn);
        }
    }
}
