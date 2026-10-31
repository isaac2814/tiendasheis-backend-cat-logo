package com.sheis.catalogo.web.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarProductoRequest(
        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId,
        @NotBlank(message = "El nombre del producto es obligatorio")
        @Size(max = 80, message = "El nombre del producto no puede superar los 80 caracteres")
        String nombre,
        String descripcion,
        String incluye,
        @Size(max = 80, message = "La garantía no puede superar los 80 caracteres")
        String garantia,
        @Size(max = 30, message = "El lema no puede superar los 30 caracteres")
        String lema,
        @Size(max = 30, message = "La etiqueta de variante no puede superar los 30 caracteres")
        String etiquetaVariante,
        Integer orden,
        Boolean visible
) {}
