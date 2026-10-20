package com.sheis.catalogo.web.dto.admin;

import java.util.List;

public record AdminServicioDto(
        String id,
        String nombre,
        String modalidad,
        String inicial,
        String color,
        String logoUrl,
        String imagenUrl,
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
        List<AdminPlanDto> planes,
        String lema,
        Integer favoritoOrden
) {
    public AdminServicioDto(String id, String nombre, String modalidad, String inicial, String color,
                            String logoUrl, String imagenUrl, String etiqueta, String descripcion,
                            List<String> incluye, List<String> recomendaciones, Boolean visible, Integer orden,
                            Integer stockManual, Integer stock, boolean agotado, Long precioBase,
                            List<AdminPlanDto> planes) {
        this(id, nombre, modalidad, inicial, color, logoUrl, imagenUrl, etiqueta, descripcion, incluye, recomendaciones, visible, orden, stockManual, stock, agotado, precioBase, planes, null, null);
    }

    public AdminServicioDto(String id, String nombre, String modalidad, String inicial, String color,
                            String logoUrl, String etiqueta, String descripcion, List<String> incluye,
                            List<String> recomendaciones, Boolean visible, Integer orden,
                            Integer stockManual, Integer stock, boolean agotado, Long precioBase,
                            List<AdminPlanDto> planes) {
        this(id, nombre, modalidad, inicial, color, logoUrl, null, etiqueta, descripcion, incluye, recomendaciones, visible, orden, stockManual, stock, agotado, precioBase, planes, null, null);
    }
}
