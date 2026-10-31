package com.sheis.catalogo.web.dto.publico;

import java.util.List;

public record ProductosPaginadosResponse(
        List<ProductoTarjetaPublicoDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {}
