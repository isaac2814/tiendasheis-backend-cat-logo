package com.sheis.catalogo.exception;

/**
 * Límite de solicitudes excedido (HTTP 429). El mensaje es siempre genérico para no
 * revelar si el correo existe ni el estado interno del limitador.
 */
public class RateLimitException extends RuntimeException {

    private static final String MENSAJE_GENERICO =
            "Demasiadas solicitudes. Inténtalo de nuevo en unos minutos.";

    public RateLimitException() {
        super(MENSAJE_GENERICO);
    }

    public RateLimitException(String message) {
        super(MENSAJE_GENERICO);
    }
}
