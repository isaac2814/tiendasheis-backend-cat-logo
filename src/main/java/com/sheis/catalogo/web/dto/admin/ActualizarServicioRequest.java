package com.sheis.catalogo.web.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record ActualizarServicioRequest(
        @NotBlank(message = "El nombre del servicio es obligatorio")
        String nombre,

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
        @jakarta.validation.constraints.Size(max = 30, message = "El lema no puede superar los 30 caracteres")
        String lema,
        Integer favoritoOrden
) {
    public ActualizarServicioRequest(String nombre, String inicial, String color, String etiqueta,
                                   String descripcion, List<String> incluye, List<String> recomendaciones,
                                   String logoUrl, Integer orden) {
        this(nombre, inicial, color, etiqueta, descripcion, incluye, recomendaciones, logoUrl, orden, null, null);
    }
}
