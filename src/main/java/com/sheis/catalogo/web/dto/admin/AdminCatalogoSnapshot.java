package com.sheis.catalogo.web.dto.admin;

import java.util.List;

public record AdminCatalogoSnapshot(
        List<AdminServicioDto> servicios,
        List<AdminComboDto> combos,
        AdminDescuentosDto descuentos,
        AdminAjustesDto ajustes
) {}
