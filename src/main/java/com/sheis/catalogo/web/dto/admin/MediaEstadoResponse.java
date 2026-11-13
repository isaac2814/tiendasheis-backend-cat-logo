package com.sheis.catalogo.web.dto.admin;

public record MediaEstadoResponse(
        boolean enabled,
        String bucket,
        String publicBaseUrl,
        boolean healthy,
        String message
) {}
