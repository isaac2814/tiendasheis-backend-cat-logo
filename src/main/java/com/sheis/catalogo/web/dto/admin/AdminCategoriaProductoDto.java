package com.sheis.catalogo.web.dto.admin;

public record AdminCategoriaProductoDto(
        Long id,
        String nombre,
        Integer orden,
        Boolean visible,
        long productosCount
) {}
