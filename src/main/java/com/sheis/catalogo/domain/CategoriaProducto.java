package com.sheis.catalogo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categoria_producto")
@Getter
@Setter
public class CategoriaProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String nombre;

    @Column(nullable = false)
    private Integer orden = 1;

    @Column(nullable = false)
    private Boolean visible = true;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn = Instant.now();

    @OneToMany(mappedBy = "categoria", cascade = CascadeType.ALL)
    private List<Producto> productos = new ArrayList<>();

    public CategoriaProducto() {
    }

    public CategoriaProducto(String nombre, Integer orden, Boolean visible) {
        this.nombre = nombre;
        this.orden = orden != null ? orden : 1;
        this.visible = visible != null ? visible : true;
        this.creadoEn = Instant.now();
        this.actualizadoEn = Instant.now();
    }
}
