package com.sheis.catalogo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "servicio_imagenes")
@Getter
@Setter
public class ServicioImagen {

    @Id
    @Column(name = "servicio_id")
    private Long servicioId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "servicio_id")
    private SkuCategoria skuCategoria;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "datos", nullable = false)
    private byte[] datos;

    @Column(name = "bytes_len", nullable = false)
    private Integer bytesLen;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn = Instant.now();

    public ServicioImagen() {
    }

    public ServicioImagen(SkuCategoria skuCategoria, String contentType, byte[] datos, Integer bytesLen, Instant actualizadoEn) {
        this.skuCategoria = skuCategoria;
        this.contentType = contentType;
        this.datos = datos;
        this.bytesLen = bytesLen;
        this.actualizadoEn = actualizadoEn != null ? actualizadoEn : Instant.now();
    }
}
