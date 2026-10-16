package com.sheis.catalogo.web.dto.auth;

public record LoginResponse(
        String token,
        String tipo,
        long expiraEnSegundos
) {}
