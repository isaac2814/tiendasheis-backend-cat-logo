package com.sheis.catalogo.web.dto.publico;

import java.time.Instant;

public record ProductoImagenMeta(
        Long id,
        Long productoId,
        Instant actualizadoEn
) {}
