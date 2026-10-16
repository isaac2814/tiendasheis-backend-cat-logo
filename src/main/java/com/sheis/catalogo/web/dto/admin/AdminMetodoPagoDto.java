package com.sheis.catalogo.web.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record AdminMetodoPagoDto(
        @NotBlank(message = "El id es obligatorio")
        String id,

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "La inicial es obligatoria")
        String inicial,

        @NotBlank(message = "El color es obligatorio")
        String color,

        @NotBlank(message = "La etiqueta del dato es obligatoria")
        String etiquetaDato,

        @NotBlank(message = "El dato de pago es obligatorio")
        String dato,

        @NotBlank(message = "El titular es obligatorio")
        String titular,

        Boolean activo,
        Integer orden
) {}
