package com.sheis.catalogo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "producto")
@Getter
@Setter
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaProducto categoria;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(columnDefinition = "TEXT")
    private String incluye;

    @Column(length = 80)
    private String garantia;

    @Column(length = 30)
    private String lema;

    @Column(name = "etiqueta_variante", nullable = false, length = 30)
    private String etiquetaVariante = "Opción";

    @Column(nullable = false)
    private Integer orden = 1;

    @Column(nullable = false)
    private Boolean visible = true;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn = Instant.now();

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductoVariante> variantes = new ArrayList<>();

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductoImagen> imagenes = new ArrayList<>();

    public Producto() {
    }

    public Producto(CategoriaProducto categoria, String nombre, String descripcion, String incluye,
                    String garantia, String lema, String etiquetaVariante, Integer orden, Boolean visible) {
        this.categoria = categoria;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.incluye = incluye;
        this.garantia = garantia;
        this.lema = lema;
        this.etiquetaVariante = (etiquetaVariante != null && !etiquetaVariante.isBlank()) ? etiquetaVariante : "Opción";
        this.orden = orden != null ? orden : 1;
        this.visible = visible != null ? visible : true;
        this.creadoEn = Instant.now();
        this.actualizadoEn = Instant.now();
    }
}
