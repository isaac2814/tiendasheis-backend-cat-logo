package com.sheis.catalogo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "combos")
@Getter
@Setter
public class Combo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String etiqueta = "";

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false)
    private Boolean visible = true;

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(name = "favorito_orden")
    private Integer favoritoOrden;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "combo_items",
        joinColumns = @JoinColumn(name = "combo_id"),
        inverseJoinColumns = @JoinColumn(name = "sku_categoria_id")
    )
    private List<SkuCategoria> servicios = new ArrayList<>();

    public Combo() {
    }

    public Combo(Long id, String etiqueta, Boolean activo, Integer orden, List<SkuCategoria> servicios) {
        this(id, etiqueta, activo, activo, orden, null, servicios);
    }

    public Combo(Long id, String etiqueta, Boolean activo, Boolean visible, Integer orden, Integer favoritoOrden, List<SkuCategoria> servicios) {
        this.id = id;
        this.etiqueta = etiqueta != null ? etiqueta : "";
        this.activo = activo != null ? activo : (visible != null ? visible : true);
        this.visible = visible != null ? visible : (activo != null ? activo : true);
        this.orden = orden != null ? orden : 0;
        this.favoritoOrden = favoritoOrden;
        this.servicios = servicios != null ? servicios : new ArrayList<>();
    }

    public static ComboBuilder builder() {
        return new ComboBuilder();
    }

    public static class ComboBuilder {
        private Long id;
        private String etiqueta = "";
        private Boolean activo = true;
        private Boolean visible = true;
        private Integer orden = 0;
        private Integer favoritoOrden;
        private List<SkuCategoria> servicios = new ArrayList<>();

        public ComboBuilder id(Long id) { this.id = id; return this; }
        public ComboBuilder etiqueta(String etiqueta) { this.etiqueta = etiqueta; return this; }
        public ComboBuilder activo(Boolean activo) {
            this.activo = activo;
            this.visible = activo;
            return this;
        }
        public ComboBuilder visible(Boolean visible) {
            this.visible = visible;
            this.activo = visible;
            return this;
        }
        public ComboBuilder orden(Integer orden) { this.orden = orden; return this; }
        public ComboBuilder favoritoOrden(Integer favoritoOrden) { this.favoritoOrden = favoritoOrden; return this; }
        public ComboBuilder servicios(List<SkuCategoria> servicios) { this.servicios = servicios; return this; }

        public Combo build() {
            return new Combo(id, etiqueta, activo, visible, orden, favoritoOrden, servicios);
        }
    }
}
