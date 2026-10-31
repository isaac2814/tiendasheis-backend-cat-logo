package com.sheis.catalogo.web.dto.publico;

public record ProductoTarjetaPublicoDto(
        Long id,
        String slug,
        String nombre,
        String lema,
        String imagen,
        long precioDesde,
        boolean tieneVariantes,
        String stockEstado,
        boolean precioVaria,
        Long categoriaId,
        String categoriaNombre,
        String categoriaSlug
) {}
