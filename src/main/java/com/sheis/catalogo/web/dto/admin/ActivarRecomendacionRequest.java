package com.sheis.catalogo.web.dto.admin;

import jakarta.validation.constraints.NotNull;

public record ActivarRecomendacionRequest(
        @NotNull(message = "El estado activa es obligatorio")
        Boolean activa
) {}
