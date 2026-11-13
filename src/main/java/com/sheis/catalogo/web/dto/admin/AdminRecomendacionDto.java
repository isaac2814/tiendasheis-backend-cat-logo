package com.sheis.catalogo.web.dto.admin;

import com.sheis.catalogo.domain.TipoRecomendacion;
import java.time.Instant;

public record AdminRecomendacionDto(
        Long id,
        String titulo,
        String descripcion,
        TipoRecomendacion tipo,
        String objectKey,
        String posterKey,
        String videoUrl,
        String posterUrl,
        String youtubeId,
        Integer duracionSeg,
        Long tamanoBytes,
        Long productoId,
        String productoNombre,
        String categoriaSlug,
        Integer orden,
        boolean activa,
        Instant creadoEn
) {}
