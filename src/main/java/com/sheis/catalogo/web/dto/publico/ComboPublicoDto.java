package com.sheis.catalogo.web.dto.publico;

import java.util.List;

public record ComboPublicoDto(
        long id,
        String etiqueta,
        List<String> servicios,
        long precioNormal,
        long descuento,
        long precio,
        boolean agotado,
        int orden,
        boolean visible
) {
    public ComboPublicoDto(long id, String etiqueta, List<String> servicios, long precioNormal,
                           long descuento, long precio, boolean agotado) {
        this(id, etiqueta, servicios, precioNormal, descuento, precio, agotado, 0, true);
    }

    public ComboPublicoDto(long id, String etiqueta, List<String> servicios, long precioNormal,
                           long descuento, long precio, boolean agotado, int orden) {
        this(id, etiqueta, servicios, precioNormal, descuento, precio, agotado, orden, true);
    }
}
