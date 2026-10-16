package com.sheis.catalogo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "servicios")
@Getter
@Setter
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    public Servicio() {
    }

    public Servicio(Long id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public static ServicioBuilder builder() {
        return new ServicioBuilder();
    }

    public static class ServicioBuilder {
        private Long id;
        private String nombre;

        public ServicioBuilder id(Long id) { this.id = id; return this; }
        public ServicioBuilder nombre(String nombre) { this.nombre = nombre; return this; }

        public Servicio build() {
            return new Servicio(id, nombre);
        }
    }
}
