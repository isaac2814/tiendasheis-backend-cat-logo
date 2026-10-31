package com.sheis.catalogo.web.dto.publico;

public record CategoriaConteoDto(
        Long id,
        String nombre,
        String slug,
        long total
) {}
