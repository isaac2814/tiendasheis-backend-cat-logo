package com.sheis.catalogo.web.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PresignRequest(
        @NotNull(message = "El tipo de archivo (kind) es obligatorio")
        MediaKind kind,

        @NotBlank(message = "El contentType es obligatorio")
        String contentType,

        @NotNull(message = "El tamaño es obligatorio")
        @Positive(message = "El tamaño debe ser mayor a 0")
        Long sizeBytes
) {}
