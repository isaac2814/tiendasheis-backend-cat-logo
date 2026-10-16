package com.sheis.catalogo.web.dto.admin;

import jakarta.validation.constraints.NotNull;

public record CambiarVisibleRequest(
        @NotNull(message = "El campo visible es obligatorio")
        Boolean visible
) {}
