package com.sheis.catalogo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "producto_imagen")
@Getter
@Setter
public class ProductoImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer orden = 1;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(name = "contenido", nullable = false)
    private byte[] contenido;

    @Column(name = "bytes_len", nullable = false)
    private Integer bytesLen;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn = Instant.now();

    public ProductoImagen() {
    }

    public ProductoImagen(Producto producto, Integer orden, String tipo, byte[] contenido, Integer bytesLen) {
        this.producto = producto;
        this.orden = orden != null ? orden : 1;
        this.tipo = tipo;
        this.contenido = contenido;
        this.bytesLen = bytesLen != null ? bytesLen : (contenido != null ? contenido.length : 0);
        this.creadoEn = Instant.now();
        this.actualizadoEn = Instant.now();
    }
}
