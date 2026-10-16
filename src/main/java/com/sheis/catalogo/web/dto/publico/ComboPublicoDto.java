package com.sheis.catalogo.web.dto.publico;

import java.util.List;

public record ComboPublicoDto(
        long id,
        String etiqueta,
        List<String> servicios,
        long precioNormal,
        long descuento,
        long precio,
        boolean agotado
) {}
