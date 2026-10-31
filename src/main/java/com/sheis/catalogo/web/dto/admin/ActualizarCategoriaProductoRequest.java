package com.sheis.catalogo.web.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarCategoriaProductoRequest(
        @NotBlank(message = "El nombre de la categoría es obligatorio")
        @Size(max = 40, message = "El nombre de la categoría no puede superar los 40 caracteres")
        String nombre,
        Integer orden,
        Boolean visible
) {}
