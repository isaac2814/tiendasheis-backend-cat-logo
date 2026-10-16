package com.sheis.catalogo.web.dto.publico;

import java.util.List;

public record ServicioPublicoDto(
        String id,
        String nombre,
        String inicial,
        String color,
        String logoUrl,
        String imagenUrl,
        String etiqueta,
        String descripcion,
        List<String> incluye,
        List<String> recomendaciones,
        Integer stock,
        boolean agotado,
        long precioBase,
        List<PlanDto> planes
) {
    public ServicioPublicoDto(String id, String nombre, String inicial, String color, String logoUrl,
                              String etiqueta, String descripcion, List<String> incluye,
                              List<String> recomendaciones, Integer stock, boolean agotado,
                              long precioBase, List<PlanDto> planes) {
        this(id, nombre, inicial, color, logoUrl, null, etiqueta, descripcion, incluye, recomendaciones, stock, agotado, precioBase, planes);
    }
}
