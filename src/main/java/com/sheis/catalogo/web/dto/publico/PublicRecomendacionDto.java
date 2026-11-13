package com.sheis.catalogo.web.dto.publico;

import com.sheis.catalogo.domain.TipoRecomendacion;

public record PublicRecomendacionDto(
        Long id,
        String titulo,
        String descripcion,
        TipoRecomendacion tipo,
        String videoUrl,
        String posterUrl,
        String youtubeId,
        Integer duracionSeg,
        Long productoId,
        String productoNombre,
        String categoriaSlug
) {}
