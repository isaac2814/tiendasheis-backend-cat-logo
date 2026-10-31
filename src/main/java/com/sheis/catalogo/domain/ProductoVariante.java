package com.sheis.catalogo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "producto_variante", uniqueConstraints = {
        @UniqueConstraint(name = "uq_producto_variante_nombre", columnNames = {"producto_id", "nombre"})
})
@Getter
@Setter
public class ProductoVariante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(nullable = false)
    private Boolean activa = true;

    @Column(nullable = false)
    private Integer orden = 1;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn = Instant.now();

    public ProductoVariante() {
    }

    public ProductoVariante(Producto producto, String nombre, String sku, BigDecimal precio,
                            Integer stock, Boolean activa, Integer orden) {
        this.producto = producto;
        this.nombre = nombre;
        this.sku = sku;
        this.precio = precio;
        this.stock = stock != null ? stock : 0;
        this.activa = activa != null ? activa : true;
        this.orden = orden != null ? orden : 1;
        this.creadoEn = Instant.now();
        this.actualizadoEn = Instant.now();
    }
}
