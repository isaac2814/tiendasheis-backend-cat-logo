package com.sheis.catalogo.web.dto.publico;

public record MetodoPagoDto(
        String id,
        String nombre,
        String inicial,
        String color,
        String etiquetaDato,
        String dato,
        String titular,
        String imagenUrl
) {}
