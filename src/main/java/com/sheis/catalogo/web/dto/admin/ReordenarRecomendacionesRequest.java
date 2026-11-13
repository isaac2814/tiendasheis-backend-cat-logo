package com.sheis.catalogo.web.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReordenarRecomendacionesRequest(
        @NotEmpty(message = "La lista de IDs no puede estar vacía")
        List<Long> ids
) {}
