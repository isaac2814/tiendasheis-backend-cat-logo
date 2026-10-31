package com.sheis.catalogo.web.dto.admin;

import com.sheis.catalogo.web.dto.publico.FavoritoDto;
import java.util.List;

public record AdminCatalogoSnapshot(
        List<AdminServicioDto> servicios,
        List<AdminComboDto> combos,
        AdminDescuentosDto descuentos,
        AdminAjustesDto ajustes,
        List<FavoritoDto> favoritos,
        List<AdminCategoriaProductoDto> categoriasProducto,
        List<AdminProductoDto> productos
) {
    public AdminCatalogoSnapshot(List<AdminServicioDto> servicios, List<AdminComboDto> combos, AdminDescuentosDto descuentos, AdminAjustesDto ajustes) {
        this(servicios, combos, descuentos, ajustes, List.of(), List.of(), List.of());
    }

    public AdminCatalogoSnapshot(List<AdminServicioDto> servicios, List<AdminComboDto> combos, AdminDescuentosDto descuentos, AdminAjustesDto ajustes, List<FavoritoDto> favoritos) {
        this(servicios, combos, descuentos, ajustes, favoritos, List.of(), List.of());
    }
}
