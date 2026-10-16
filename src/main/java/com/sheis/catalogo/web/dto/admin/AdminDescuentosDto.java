package com.sheis.catalogo.web.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record AdminDescuentosDto(
        @NotNull(message = "El mapa de descuentos por cantidad es obligatorio")
        Map<String, Long> porCantidad,

        @Min(value = 0, message = "El descuento extra desde el 6to debe ser mayor o igual a 0")
        long extraDesdeSexto
) {}
