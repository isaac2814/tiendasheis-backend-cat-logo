package com.sheis.catalogo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "producto_video")
@Getter
@Setter
public class ProductoVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false, unique = true)
    private Producto producto;

    @Column(name = "object_key", nullable = false, length = 255)
    private String objectKey;

    @Column(name = "poster_key", nullable = false, length = 255)
    private String posterKey;

    @Column(name = "duracion_seg", nullable = false)
    private Integer duracionSeg;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    public ProductoVideo() {
    }

    public ProductoVideo(Producto producto, String objectKey, String posterKey, Integer duracionSeg, Long tamanoBytes) {
        this.producto = producto;
        this.objectKey = objectKey;
        this.posterKey = posterKey;
        this.duracionSeg = duracionSeg;
        this.tamanoBytes = tamanoBytes;
        this.creadoEn = Instant.now();
    }
}
