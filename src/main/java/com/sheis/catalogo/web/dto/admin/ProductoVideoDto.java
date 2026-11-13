package com.sheis.catalogo.web.dto.admin;

public record ProductoVideoDto(
        Long id,
        Long productoId,
        String objectKey,
        String posterKey,
        String videoUrl,
        String posterUrl,
        Integer duracionSeg,
        Long tamanoBytes
) {}
