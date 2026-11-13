package com.sheis.catalogo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "recomendacion")
@Getter
@Setter
public class Recomendacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String titulo;

    @Column(length = 140)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoRecomendacion tipo;

    @Column(name = "object_key", length = 255)
    private String objectKey;

    @Column(name = "poster_key", length = 255)
    private String posterKey;

    @Column(name = "youtube_id", length = 30)
    private String youtubeId;

    @Column(name = "duracion_seg")
    private Integer duracionSeg;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(name = "categoria_slug", length = 60)
    private String categoriaSlug;

    @Column(nullable = false)
    private Integer orden = 1;

    @Column(nullable = false)
    private Boolean activa = true;

    public boolean isActiva() {
        return Boolean.TRUE.equals(activa);
    }

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    public Recomendacion() {
    }

    public Recomendacion(String titulo, String descripcion, TipoRecomendacion tipo,
                         String objectKey, String posterKey, String youtubeId,
                         Integer duracionSeg, Long tamanoBytes,
                         Producto producto, String categoriaSlug,
                         Integer orden, Boolean activa) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.objectKey = objectKey;
        this.posterKey = posterKey;
        this.youtubeId = youtubeId;
        this.duracionSeg = duracionSeg;
        this.tamanoBytes = tamanoBytes;
        this.producto = producto;
        this.categoriaSlug = categoriaSlug;
        this.orden = orden != null ? orden : 1;
        this.activa = activa != null ? activa : true;
        this.creadoEn = Instant.now();
    }
}
