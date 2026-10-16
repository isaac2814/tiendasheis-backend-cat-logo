package com.sheis.catalogo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "metodos_pago")
@Getter
@Setter
public class MetodoPago {

    @Id
    @Column(nullable = false)
    private String id; // 'nequi', 'breb'

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, length = 2)
    private String inicial;

    @Column(nullable = false, length = 7)
    private String color;

    @Column(name = "etiqueta_dato", nullable = false)
    private String etiquetaDato;

    @Column(nullable = false)
    private String dato;

    @Column(nullable = false)
    private String titular;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false)
    private Integer orden = 0;

    public MetodoPago() {
    }

    public MetodoPago(String id, String nombre, String inicial, String color, String etiquetaDato,
                      String dato, String titular, Boolean activo, Integer orden) {
        this.id = id;
        this.nombre = nombre;
        this.inicial = inicial;
        this.color = color;
        this.etiquetaDato = etiquetaDato;
        this.dato = dato;
        this.titular = titular;
        this.activo = activo != null ? activo : true;
        this.orden = orden != null ? orden : 0;
    }

    public static MetodoPagoBuilder builder() {
        return new MetodoPagoBuilder();
    }

    public static class MetodoPagoBuilder {
        private String id;
        private String nombre;
        private String inicial;
        private String color;
        private String etiquetaDato;
        private String dato;
        private String titular;
        private Boolean activo = true;
        private Integer orden = 0;

        public MetodoPagoBuilder id(String id) { this.id = id; return this; }
        public MetodoPagoBuilder nombre(String nombre) { this.nombre = nombre; return this; }
        public MetodoPagoBuilder inicial(String inicial) { this.inicial = inicial; return this; }
        public MetodoPagoBuilder color(String color) { this.color = color; return this; }
        public MetodoPagoBuilder etiquetaDato(String etiquetaDato) { this.etiquetaDato = etiquetaDato; return this; }
        public MetodoPagoBuilder dato(String dato) { this.dato = dato; return this; }
        public MetodoPagoBuilder titular(String titular) { this.titular = titular; return this; }
        public MetodoPagoBuilder activo(Boolean activo) { this.activo = activo; return this; }
        public MetodoPagoBuilder orden(Integer orden) { this.orden = orden; return this; }

        public MetodoPago build() {
            return new MetodoPago(id, nombre, inicial, color, etiquetaDato, dato, titular, activo, orden);
        }
    }
}
