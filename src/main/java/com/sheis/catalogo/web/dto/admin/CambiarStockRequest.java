package com.sheis.catalogo.web.dto.admin;

import jakarta.validation.constraints.Min;

public record CambiarStockRequest(
        @Min(value = 0, message = "El stock debe ser mayor o igual a 0")
        Integer stock
) {}
