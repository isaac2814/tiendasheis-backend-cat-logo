package com.sheis.catalogo.web.dto.publico;

public record VariantePublicoDto(
        Long id,
        String nombre,
        Long precio,
        Integer stock
) {}
