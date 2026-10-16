package com.sheis.catalogo.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Cors cors = new Cors();
    private Jwt jwt = new Jwt();
    private Admin admin = new Admin();
    private Proxy proxy = new Proxy();

    @Getter
    @Setter
    public static class Cors {
        private String allowedOrigins = "http://localhost:5173";

        public List<String> getAllowedOriginsList() {
            if (allowedOrigins == null || allowedOrigins.isBlank()) {
                return List.of("http://localhost:5173");
            }
            return Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret = "";
        private int ttlHours = 8;
    }

    @Getter
    @Setter
    public static class Admin {
        private String email = "";
        private String password = "";
    }

    @Getter
    @Setter
    public static class Proxy {
        /**
         * Activar SOLO si la aplicación está detrás de un proxy inverso de confianza
         * (nginx, Traefik, balanceador, etc.) que sobrescribe {@code X-Forwarded-For}.
         * Si es {@code false} (por defecto) se usa la IP real del socket, evitando que
         * un cliente falsifique su IP para eludir el límite de intentos de login.
         */
        private boolean trustForwardedHeaders = false;
    }

    @PostConstruct
    public void validate() {
        if (jwt.getSecret() == null || jwt.getSecret().trim().length() < 32) {
            throw new IllegalStateException("app.jwt.secret debe configurarse con al menos 32 caracteres (variable de entorno JWT_SECRET)");
        }
    }
}
