package com.sheis.catalogo.web.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record CrearServicioRequest(
        @NotBlank(message = "El nombre del servicio es obligatorio")
        String nombre,

        String codigo,

        String modalidad,

        @NotBlank(message = "La inicial es obligatoria")
        @Pattern(regexp = "^[A-Za-z0-9]{1,2}$", message = "La inicial debe tener 1 o 2 caracteres alfanuméricos")
        String inicial,

        @NotBlank(message = "El color hexadecimal es obligatorio")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe tener formato #RRGGBB")
        String color,

        String etiqueta,
        String descripcion,
        List<String> incluye,
        List<String> recomendaciones,
        String logoUrl,
        Integer orden,
        Boolean visible,
        Integer stockManual
) {}
