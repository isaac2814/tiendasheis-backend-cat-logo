package com.sheis.catalogo.web.dto.publico;

import java.util.List;

public record ProductoPublicoDto(
        Long id,
        String nombre,
        Long categoriaId,
        String lema,
        String descripcion,
        List<String> incluye,
        String garantia,
        String etiquetaVariante,
        List<String> imagenes,
        List<VariantePublicoDto> variantes
) {}
