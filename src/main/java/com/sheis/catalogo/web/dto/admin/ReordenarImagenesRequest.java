package com.sheis.catalogo.web.dto.admin;

import java.util.List;

public record ReordenarImagenesRequest(
        List<Long> imagenesIds
) {}
