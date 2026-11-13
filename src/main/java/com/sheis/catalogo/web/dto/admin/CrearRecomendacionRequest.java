package com.sheis.catalogo.web.dto.admin;

import com.sheis.catalogo.domain.TipoRecomendacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearRecomendacionRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 80, message = "El título no puede exceder 80 caracteres")
        String titulo,

        @Size(max = 140, message = "La descripción no puede exceder 140 caracteres")
        String descripcion,

        @NotNull(message = "El tipo es obligatorio")
        TipoRecomendacion tipo,

        String objectKey,
        String posterKey,
        String youtubeUrlOrId,
        Integer duracionSeg,
        Long productoId,
        String categoriaSlug,
        Integer orden,
        Boolean activa
) {}
