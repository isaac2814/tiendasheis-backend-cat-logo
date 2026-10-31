package com.sheis.catalogo.web.dto.publico;

import java.util.List;

public record TecnologiaPublicoDto(
        List<CategoriaProductoPublicoDto> categorias,
        List<ProductoPublicoDto> productos
) {
    public TecnologiaPublicoDto() {
        this(List.of(), List.of());
    }
}
