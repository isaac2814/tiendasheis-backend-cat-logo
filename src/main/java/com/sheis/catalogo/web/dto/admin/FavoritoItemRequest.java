package com.sheis.catalogo.web.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record FavoritoItemRequest(
        @NotBlank(message = "El tipo de favorito es obligatorio")
        String tipo,
        @NotBlank(message = "La referencia del favorito es obligatoria")
        String ref
) {}
