package com.sheis.catalogo.web.dto.admin;

import java.util.List;

public record EliminarServicioResponse(
        String mensaje,
        List<Long> combosAfectados
) {}
