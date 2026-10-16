package com.sheis.catalogo.web;

import com.sheis.catalogo.config.AppProperties;
import com.sheis.catalogo.security.AuthService;
import com.sheis.catalogo.web.dto.auth.LoginRequest;
import com.sheis.catalogo.web.dto.auth.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Endpoints de autenticación para administradores")
public class AuthController {

    private final AuthService authService;
    private final AppProperties appProperties;

    public AuthController(AuthService authService, AppProperties appProperties) {
        this.authService = authService;
        this.appProperties = appProperties;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión como administrador", description = "Valida credenciales y retorna un JWT Bearer token con rol ADMIN")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        return ResponseEntity.ok(authService.login(request, clientIp));
    }

    /**
     * La cabecera {@code X-Forwarded-For} solo se respeta si {@code app.proxy.trust-forwarded-headers}
     * está activado (la app está detrás de un proxy de confianza que la sobrescribe). En caso contrario
     * se usa la IP real del socket, para que un cliente no pueda falsificar su IP y eludir el rate limiter.
     */
    private String extractClientIp(HttpServletRequest request) {
        if (!appProperties.getProxy().isTrustForwardedHeaders()) {
            return request.getRemoteAddr();
        }
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
