package com.sheis.catalogo.web.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AdminComboInputDto(
        Long id,
        String etiqueta,
        Boolean activo,
        @NotEmpty(message = "El combo debe incluir al menos un servicio")
        List<String> servicios,
        Integer orden,
        Boolean visible,
        String badge,
        Integer favoritoOrden
) {
    public AdminComboInputDto(Long id, String etiqueta, Boolean activo, List<String> servicios, Integer orden) {
        this(id, etiqueta, activo, servicios, orden, activo, etiqueta, null);
    }

    public String getEtiquetaEfectiva() {
        if (badge != null && !badge.isBlank()) return badge.trim();
        if (etiqueta != null) return etiqueta.trim();
        return "";
    }

    public Boolean getVisibleEfectivo() {
        if (visible != null) return visible;
        if (activo != null) return activo;
        return true;
    }
}
