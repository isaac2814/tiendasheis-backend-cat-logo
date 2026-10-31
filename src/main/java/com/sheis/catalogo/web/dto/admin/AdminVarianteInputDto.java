package com.sheis.catalogo.web.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AdminVarianteInputDto(
        Long id,
        @NotBlank(message = "El nombre de la variante es obligatorio")
        @Size(max = 60, message = "El nombre de la variante no puede superar los 60 caracteres")
        String nombre,
        @Size(max = 50, message = "El SKU no puede superar los 50 caracteres")
        String sku,
        @NotNull(message = "El precio de la variante es obligatorio")
        @Positive(message = "El precio de la variante debe ser mayor a 0")
        BigDecimal precio,
        Integer stock,
        Boolean activa,
        Integer orden
) {}
