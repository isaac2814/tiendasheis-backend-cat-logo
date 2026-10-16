package com.sheis.catalogo.web.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AdminPlanDto(
        @NotNull(message = "Los días son obligatorios")
        @Min(value = 1, message = "Los días deben ser mayores a 0")
        Integer dias,

        @NotNull(message = "La cantidad de perfiles es obligatoria")
        @Min(value = 1, message = "La cantidad de perfiles debe ser mayor a 0")
        Integer perfiles,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", message = "El precio debe ser mayor o igual a 0")
        BigDecimal precio,

        Boolean activo
) {}
