package com.sheis.catalogo.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Getter
@Setter
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private static final int JWT_SECRET_MIN_LENGTH = 32;
    private static final int ADMIN_PASSWORD_MIN_LENGTH = 10;

    /** Origen usado solo cuando no se configura ningún origen (desarrollo local). */
    private static final String DEFAULT_ORIGIN = "http://localhost:5173";

    /**
     * Valores de secreto de ejemplo/desarrollo que JAMÁS deben usarse en un despliegue real.
     * Si el secreto configurado coincide con alguno, la aplicación se niega a arrancar.
     */
    private static final Set<String> JWT_SECRETS_PROHIBIDOS = Set.of(
            "super_secret_jwt_key_at_least_32_characters_123456",
            "PEGA_AQUI_UN_SECRETO_ALEATORIO_DE_32_CARACTERES_O_MAS",
            "changeme",
            "change_me",
            "secret",
            "secretsecretsecretsecretsecretsecret"
    );

    private Cors cors = new Cors();
    private Jwt jwt = new Jwt();
    private Admin admin = new Admin();
    private Proxy proxy = new Proxy();
    private Security security = new Security();

    @Getter
    @Setter
    public static class Cors {
        private String allowedOrigins = DEFAULT_ORIGIN;

        /**
         * Devuelve los orígenes permitidos ya normalizados: sin espacios ni saltos de
         * línea alrededor, sin barra final, en minúsculas, sin entradas vacías y sin
         * duplicados. Se admiten varios orígenes separados por coma (o por espacios /
         * saltos de línea). La comparación posterior es insensible a mayúsculas.
         */
        public List<String> getAllowedOriginsList() {
            if (allowedOrigins == null || allowedOrigins.isBlank()) {
                return List.of(DEFAULT_ORIGIN);
            }
            return Arrays.stream(allowedOrigins.split("[,\\s]+"))
                    .map(String::strip)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.replaceAll("/+$", ""))
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .distinct()
                    .toList();
        }
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret = "";
        /** Caducidad del token en minutos. Configurable con JWT_EXPIRATION_MINUTES (por defecto 8 h). */
        private long ttlMinutes = 480;
    }

    @Getter
    @Setter
    public static class Admin {
        private String email = "";
        private String password = "";
        /**
         * Restablecimiento opcional de la contraseña al arrancar (ADMIN_RESET_PASSWORD).
         * Solo se activa con el valor {@code true} (sin distinguir mayúsculas); cualquier
         * otro valor mantiene el comportamiento normal.
         */
        private String resetPassword = "";

        public boolean isResetPasswordEnabled() {
            return resetPassword != null && resetPassword.trim().equalsIgnoreCase("true");
        }
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

    @Getter
    @Setter
    public static class Security {
        /**
         * Permite el acceso sin token a Swagger/OpenAPI. Útil en desarrollo; en producción
         * se desactiva (springdoc deshabilitado + perfil prod con este flag en {@code false}).
         */
        private boolean permitSwagger = true;
    }

    @PostConstruct
    public void validate() {
        validarJwt();
        validarAdmin();
        List<String> origenes = cors.getAllowedOriginsList();
        log.info("CORS: orígenes permitidos ({}): {}", origenes.size(), origenes);
    }

    private void validarJwt() {
        String secret = jwt.getSecret() == null ? "" : jwt.getSecret().trim();
        if (secret.isEmpty()) {
            throw new IllegalStateException(
                    "Falta JWT_SECRET: configura un secreto aleatorio de al menos "
                            + JWT_SECRET_MIN_LENGTH + " caracteres.");
        }
        if (secret.length() < JWT_SECRET_MIN_LENGTH) {
            throw new IllegalStateException(
                    "JWT_SECRET es demasiado corto: debe tener al menos " + JWT_SECRET_MIN_LENGTH
                            + " caracteres (tiene " + secret.length() + ").");
        }
        if (JWT_SECRETS_PROHIBIDOS.contains(secret)) {
            throw new IllegalStateException(
                    "JWT_SECRET usa un valor de ejemplo/desarrollo conocido. Genera un secreto "
                            + "aleatorio propio (por ejemplo: openssl rand -base64 48).");
        }
    }

    private void validarAdmin() {
        String email = admin.getEmail() == null ? "" : admin.getEmail().trim();
        String password = admin.getPassword() == null ? "" : admin.getPassword();
        if (email.isEmpty() || password.isEmpty()) {
            throw new IllegalStateException(
                    "Faltan ADMIN_EMAIL y/o ADMIN_PASSWORD: se requieren para crear el "
                            + "administrador inicial cuando la tabla de usuarios esté vacía.");
        }
        if (password.length() < ADMIN_PASSWORD_MIN_LENGTH) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD es demasiado débil: debe tener al menos "
                            + ADMIN_PASSWORD_MIN_LENGTH + " caracteres.");
        }
    }
}
