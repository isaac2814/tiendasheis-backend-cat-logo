package com.sheis.catalogo.web.dto.publico;

import java.util.Map;

public record DescuentosDto(
        Map<String, Long> porCantidad,
        long extraDesdeSexto
) {}
