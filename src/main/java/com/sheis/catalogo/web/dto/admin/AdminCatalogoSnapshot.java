package com.sheis.catalogo.web.dto.admin;

import com.sheis.catalogo.web.dto.publico.FavoritoDto;
import java.util.List;

public record AdminCatalogoSnapshot(
        List<AdminServicioDto> servicios,
        List<AdminComboDto> combos,
        AdminDescuentosDto descuentos,
        AdminAjustesDto ajustes,
        List<FavoritoDto> favoritos
) {
    public AdminCatalogoSnapshot(List<AdminServicioDto> servicios, List<AdminComboDto> combos, AdminDescuentosDto descuentos, AdminAjustesDto ajustes) {
        this(servicios, combos, descuentos, ajustes, List.of());
    }
}
