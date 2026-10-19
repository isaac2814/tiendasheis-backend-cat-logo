package com.sheis.catalogo.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link CorsConfiguration} que compara el origen de la petición sin distinguir
 * mayúsculas y sin barra final. Los orígenes permitidos se guardan ya normalizados
 * (minúsculas, sin barra final) y el {@code Access-Control-Allow-Origin} se devuelve
 * con el valor exacto enviado por el navegador.
 *
 * <p>Además deja un {@code WARN} cuando se rechaza una petición por origen, con el
 * origen recibido y la lista de orígenes permitidos.
 */
@Slf4j
public class NormalizedCorsConfiguration extends CorsConfiguration {

    private final Set<String> allowedOriginsNormalized;

    public NormalizedCorsConfiguration(List<String> allowedOrigins) {
        super();
        this.allowedOriginsNormalized = allowedOrigins.stream()
                .map(NormalizedCorsConfiguration::normalize)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
        setAllowedOrigins(List.copyOf(this.allowedOriginsNormalized));
    }

    @Override
    @Nullable
    public String checkOrigin(@Nullable String requestOrigin) {
        if (requestOrigin == null) {
            return null;
        }
        String normalized = normalize(requestOrigin);
        if (normalized != null && allowedOriginsNormalized.contains(normalized)) {
            // Se devuelve el origen tal cual lo envió el navegador (solo sin espacios).
            return requestOrigin.strip();
        }
        log.warn("CORS: origen rechazado '{}'. Orígenes permitidos: {}",
                requestOrigin, allowedOriginsNormalized);
        return null;
    }

    @Nullable
    private static String normalize(String origin) {
        if (origin == null) {
            return null;
        }
        String value = origin.strip().replaceAll("/+$", "");
        if (value.isEmpty()) {
            return null;
        }
        return value.toLowerCase(Locale.ROOT);
    }
}
