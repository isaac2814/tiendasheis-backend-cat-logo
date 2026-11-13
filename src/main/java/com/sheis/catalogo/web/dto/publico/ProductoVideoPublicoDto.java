package com.sheis.catalogo.web.dto.publico;

public record ProductoVideoPublicoDto(
        String videoUrl,
        String posterUrl,
        Integer duracionSeg
) {}
