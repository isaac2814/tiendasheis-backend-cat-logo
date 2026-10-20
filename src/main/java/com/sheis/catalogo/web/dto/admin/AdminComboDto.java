package com.sheis.catalogo.web.dto.admin;

import java.util.List;

public record AdminComboDto(
        Long id,
        String etiqueta,
        Boolean activo,
        Integer orden,
        List<String> servicios,
        long precioNormal,
        long descuento,
        long precio,
        boolean agotado,
        Boolean visible,
        String badge,
        Integer favoritoOrden
) {
    public AdminComboDto(Long id, String etiqueta, Boolean activo, Integer orden,
                         List<String> servicios, long precioNormal, long descuento,
                         long precio, boolean agotado) {
        this(id, etiqueta, activo, orden, servicios, precioNormal, descuento, precio, agotado, activo, etiqueta, null);
    }
}
