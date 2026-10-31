package com.sheis.catalogo.web.dto.admin;

import java.math.BigDecimal;

public record AdminProductoVarianteDto(
        Long id,
        String nombre,
        String sku,
        BigDecimal precio,
        Integer stock,
        Boolean activa,
        Integer orden
) {}
