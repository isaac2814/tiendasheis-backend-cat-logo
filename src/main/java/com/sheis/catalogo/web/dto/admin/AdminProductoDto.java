package com.sheis.catalogo.web.dto.admin;

import java.util.List;

public record AdminProductoDto(
        Long id,
        Long categoriaId,
        String categoriaNombre,
        String nombre,
        String descripcion,
        String incluye,
        String garantia,
        String lema,
        String etiquetaVariante,
        Integer orden,
        Boolean visible,
        List<AdminProductoImagenDto> imagenes,
        List<AdminProductoVarianteDto> variantes
) {}
