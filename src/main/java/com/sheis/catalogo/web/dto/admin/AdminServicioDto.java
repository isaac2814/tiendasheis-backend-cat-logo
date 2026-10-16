package com.sheis.catalogo.web.dto.admin;

import java.util.List;

public record AdminServicioDto(
        String id,
        String nombre,
        String modalidad,
        String inicial,
        String color,
        String logoUrl,
        String etiqueta,
        String descripcion,
        List<String> incluye,
        List<String> recomendaciones,
        Boolean visible,
        Integer orden,
        Integer stockManual,
        Integer stock,
        boolean agotado,
        Long precioBase,
        List<AdminPlanDto> planes
) {}
