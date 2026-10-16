package com.sheis.catalogo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "descuentos_combo")
@Getter
@Setter
public class DescuentoCombo {

    @Id
    @Column(name = "cantidad_servicios", nullable = false)
    private Integer cantidadServicios;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento;

    @Column(nullable = false)
    private Boolean activo = true;

    public DescuentoCombo() {
    }

    public DescuentoCombo(Integer cantidadServicios, BigDecimal descuento, Boolean activo) {
        this.cantidadServicios = cantidadServicios;
        this.descuento = descuento;
        this.activo = activo != null ? activo : true;
    }

    public static DescuentoComboBuilder builder() {
        return new DescuentoComboBuilder();
    }

    public static class DescuentoComboBuilder {
        private Integer cantidadServicios;
        private BigDecimal descuento;
        private Boolean activo = true;

        public DescuentoComboBuilder cantidadServicios(Integer cantidadServicios) { this.cantidadServicios = cantidadServicios; return this; }
        public DescuentoComboBuilder descuento(BigDecimal descuento) { this.descuento = descuento; return this; }
        public DescuentoComboBuilder activo(Boolean activo) { this.activo = activo; return this; }

        public DescuentoCombo build() {
            return new DescuentoCombo(cantidadServicios, descuento, activo);
        }
    }
}
