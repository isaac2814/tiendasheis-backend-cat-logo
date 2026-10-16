package com.sheis.catalogo.web.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AdminComboInputDto(
        Long id,
        String etiqueta,
        Boolean activo,
        @NotEmpty(message = "El combo debe incluir al menos un servicio")
        List<String> servicios,
        Integer orden
) {}
