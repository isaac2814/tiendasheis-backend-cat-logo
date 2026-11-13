package com.sheis.catalogo.web.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GuardarVideoProductoRequest(
        @NotBlank(message = "El objectKey del video es obligatorio")
        String objectKey,

        @NotBlank(message = "El posterKey es obligatorio")
        String posterKey,

        @NotNull(message = "La duración en segundos es obligatoria")
        @Positive(message = "La duración debe ser mayor a 0")
        Integer duracionSeg
) {}
