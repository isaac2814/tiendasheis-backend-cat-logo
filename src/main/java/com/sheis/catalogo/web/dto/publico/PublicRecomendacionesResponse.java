package com.sheis.catalogo.web.dto.publico;

import java.util.List;

public record PublicRecomendacionesResponse(
        List<PublicRecomendacionDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {}
