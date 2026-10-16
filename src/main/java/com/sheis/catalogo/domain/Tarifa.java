package com.sheis.catalogo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
    name = "tarifas",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_tarifa", columnNames = {"sku_categoria_id", "dias", "cantidad_perfiles"})
    }
)
@Getter
@Setter
public class Tarifa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_categoria_id", nullable = false)
    private SkuCategoria skuCategoria;

    @Column(nullable = false)
    private Integer dias;

    @Column(name = "cantidad_perfiles", nullable = false)
    private Integer cantidadPerfiles;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "precio_distribuidor", precision = 12, scale = 2)
    private BigDecimal precioDistribuidor;

    @Column(nullable = false)
    private Boolean activo = true;

    public Tarifa() {
    }

    public Tarifa(Long id, SkuCategoria skuCategoria, Integer dias, Integer cantidadPerfiles,
                  BigDecimal precio, BigDecimal precioDistribuidor, Boolean activo) {
        this.id = id;
        this.skuCategoria = skuCategoria;
        this.dias = dias;
        this.cantidadPerfiles = cantidadPerfiles;
        this.precio = precio;
        this.precioDistribuidor = precioDistribuidor;
        this.activo = activo != null ? activo : true;
    }

    public static TarifaBuilder builder() {
        return new TarifaBuilder();
    }

    public static class TarifaBuilder {
        private Long id;
        private SkuCategoria skuCategoria;
        private Integer dias;
        private Integer cantidadPerfiles;
        private BigDecimal precio;
        private BigDecimal precioDistribuidor;
        private Boolean activo = true;

        public TarifaBuilder id(Long id) { this.id = id; return this; }
        public TarifaBuilder skuCategoria(SkuCategoria skuCategoria) { this.skuCategoria = skuCategoria; return this; }
        public TarifaBuilder dias(Integer dias) { this.dias = dias; return this; }
        public TarifaBuilder cantidadPerfiles(Integer cantidadPerfiles) { this.cantidadPerfiles = cantidadPerfiles; return this; }
        public TarifaBuilder precio(BigDecimal precio) { this.precio = precio; return this; }
        public TarifaBuilder precioDistribuidor(BigDecimal precioDistribuidor) { this.precioDistribuidor = precioDistribuidor; return this; }
        public TarifaBuilder activo(Boolean activo) { this.activo = activo; return this; }

        public Tarifa build() {
            return new Tarifa(id, skuCategoria, dias, cantidadPerfiles, precio, precioDistribuidor, activo);
        }
    }
}
