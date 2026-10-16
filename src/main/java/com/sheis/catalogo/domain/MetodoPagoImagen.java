package com.sheis.catalogo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "metodo_pago_imagenes")
@Getter
@Setter
public class MetodoPagoImagen {

    @Id
    @Column(name = "metodo_pago_id")
    private String metodoPagoId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "metodo_pago_id")
    private MetodoPago metodoPago;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "datos", nullable = false)
    private byte[] datos;

    @Column(name = "bytes_len", nullable = false)
    private Integer bytesLen;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn = Instant.now();

    public MetodoPagoImagen() {
    }

    public MetodoPagoImagen(MetodoPago metodoPago, String contentType, byte[] datos, Integer bytesLen, Instant actualizadoEn) {
        this.metodoPago = metodoPago;
        this.contentType = contentType;
        this.datos = datos;
        this.bytesLen = bytesLen;
        this.actualizadoEn = actualizadoEn != null ? actualizadoEn : Instant.now();
    }
}
