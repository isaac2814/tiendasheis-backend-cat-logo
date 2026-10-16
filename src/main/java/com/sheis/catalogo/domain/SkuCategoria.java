package com.sheis.catalogo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sku_categorias")
@Getter
@Setter
public class SkuCategoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    @Column(nullable = false, unique = true, updatable = false)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String modalidad;

    @Column(nullable = false, length = 2)
    private String inicial;

    @Column(nullable = false, length = 7)
    private String color;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(nullable = false)
    private String etiqueta = "";

    @Column(nullable = false)
    private String descripcion = "";

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "incluye")
    private List<String> incluye = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "recomendaciones")
    private List<String> recomendaciones = new ArrayList<>();

    @Column(nullable = false)
    private Boolean visible = true;

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(name = "stock_manual")
    private Integer stockManual;

    @OneToMany(mappedBy = "skuCategoria", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Tarifa> tarifas = new ArrayList<>();

    public SkuCategoria() {
    }

    public SkuCategoria(Long id, Servicio servicio, String codigo, String nombre, String modalidad,
                        String inicial, String color, String logoUrl, String etiqueta, String descripcion,
                        List<String> incluye, List<String> recomendaciones, Boolean visible, Integer orden,
                        Integer stockManual, List<Tarifa> tarifas) {
        this.id = id;
        this.servicio = servicio;
        this.codigo = codigo;
        this.nombre = nombre;
        this.modalidad = modalidad;
        this.inicial = inicial;
        this.color = color;
        this.logoUrl = logoUrl;
        this.etiqueta = etiqueta != null ? etiqueta : "";
        this.descripcion = descripcion != null ? descripcion : "";
        this.incluye = incluye != null ? incluye : new ArrayList<>();
        this.recomendaciones = recomendaciones != null ? recomendaciones : new ArrayList<>();
        this.visible = visible != null ? visible : true;
        this.orden = orden != null ? orden : 0;
        this.stockManual = stockManual;
        this.tarifas = tarifas != null ? tarifas : new ArrayList<>();
    }

    public static SkuCategoriaBuilder builder() {
        return new SkuCategoriaBuilder();
    }

    public static class SkuCategoriaBuilder {
        private Long id;
        private Servicio servicio;
        private String codigo;
        private String nombre;
        private String modalidad;
        private String inicial;
        private String color;
        private String logoUrl;
        private String etiqueta = "";
        private String descripcion = "";
        private List<String> incluye = new ArrayList<>();
        private List<String> recomendaciones = new ArrayList<>();
        private Boolean visible = true;
        private Integer orden = 0;
        private Integer stockManual;
        private List<Tarifa> tarifas = new ArrayList<>();

        public SkuCategoriaBuilder id(Long id) { this.id = id; return this; }
        public SkuCategoriaBuilder servicio(Servicio servicio) { this.servicio = servicio; return this; }
        public SkuCategoriaBuilder codigo(String codigo) { this.codigo = codigo; return this; }
        public SkuCategoriaBuilder nombre(String nombre) { this.nombre = nombre; return this; }
        public SkuCategoriaBuilder modalidad(String modalidad) { this.modalidad = modalidad; return this; }
        public SkuCategoriaBuilder inicial(String inicial) { this.inicial = inicial; return this; }
        public SkuCategoriaBuilder color(String color) { this.color = color; return this; }
        public SkuCategoriaBuilder logoUrl(String logoUrl) { this.logoUrl = logoUrl; return this; }
        public SkuCategoriaBuilder etiqueta(String etiqueta) { this.etiqueta = etiqueta; return this; }
        public SkuCategoriaBuilder descripcion(String descripcion) { this.descripcion = descripcion; return this; }
        public SkuCategoriaBuilder incluye(List<String> incluye) { this.incluye = incluye; return this; }
        public SkuCategoriaBuilder recomendaciones(List<String> recomendaciones) { this.recomendaciones = recomendaciones; return this; }
        public SkuCategoriaBuilder visible(Boolean visible) { this.visible = visible; return this; }
        public SkuCategoriaBuilder orden(Integer orden) { this.orden = orden; return this; }
        public SkuCategoriaBuilder stockManual(Integer stockManual) { this.stockManual = stockManual; return this; }
        public SkuCategoriaBuilder tarifas(List<Tarifa> tarifas) { this.tarifas = tarifas; return this; }

        public SkuCategoria build() {
            return new SkuCategoria(id, servicio, codigo, nombre, modalidad, inicial, color, logoUrl,
                    etiqueta, descripcion, incluye, recomendaciones, visible, orden, stockManual, tarifas);
        }
    }
}
