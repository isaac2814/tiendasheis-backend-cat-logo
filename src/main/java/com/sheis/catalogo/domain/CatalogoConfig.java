package com.sheis.catalogo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "catalogo_config")
@Getter
@Setter
public class CatalogoConfig {

    @Id
    @Column(nullable = false)
    private String clave;

    @Column(nullable = false, columnDefinition = "text")
    private String valor;

    public CatalogoConfig() {
    }

    public CatalogoConfig(String clave, String valor) {
        this.clave = clave;
        this.valor = valor;
    }

    public static CatalogoConfigBuilder builder() {
        return new CatalogoConfigBuilder();
    }

    public static class CatalogoConfigBuilder {
        private String clave;
        private String valor;

        public CatalogoConfigBuilder clave(String clave) { this.clave = clave; return this; }
        public CatalogoConfigBuilder valor(String valor) { this.valor = valor; return this; }

        public CatalogoConfig build() {
            return new CatalogoConfig(clave, valor);
        }
    }
}
