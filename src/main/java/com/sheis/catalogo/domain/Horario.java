package com.sheis.catalogo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "horarios")
@Getter
@Setter
public class Horario {

    @Id
    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false)
    private String dia;

    @Column(nullable = false)
    private String texto;

    public Horario() {
    }

    public Horario(Integer orden, String dia, String texto) {
        this.orden = orden;
        this.dia = dia;
        this.texto = texto;
    }

    public static HorarioBuilder builder() {
        return new HorarioBuilder();
    }

    public static class HorarioBuilder {
        private Integer orden;
        private String dia;
        private String texto;

        public HorarioBuilder orden(Integer orden) { this.orden = orden; return this; }
        public HorarioBuilder dia(String dia) { this.dia = dia; return this; }
        public HorarioBuilder texto(String texto) { this.texto = texto; return this; }

        public Horario build() {
            return new Horario(orden, dia, texto);
        }
    }
}
