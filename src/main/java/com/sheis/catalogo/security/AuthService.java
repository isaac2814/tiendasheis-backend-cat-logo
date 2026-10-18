package com.sheis.catalogo.security;

import com.sheis.catalogo.domain.AdminUsuario;
import com.sheis.catalogo.exception.RateLimitException;
import com.sheis.catalogo.repository.AdminUsuarioRepository;
import com.sheis.catalogo.web.dto.auth.LoginRequest;
import com.sheis.catalogo.web.dto.auth.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AdminUsuarioRepository adminUsuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LoginRateLimiter rateLimiter;

    /**
     * Hash BCrypt de relleno: se compara siempre, incluso cuando el correo no existe, para que el
     * tiempo de respuesta no revele si un usuario está registrado (evita enumeración por timing).
     */
    private final String dummyPasswordHash;

    public AuthService(AdminUsuarioRepository adminUsuarioRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       LoginRateLimiter rateLimiter) {
        this.adminUsuarioRepository = adminUsuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.rateLimiter = rateLimiter;
        this.dummyPasswordHash = passwordEncoder.encode("tiendasheis-timing-equalizer");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request, String clientIp) {
        if (rateLimiter.estaBloqueado(clientIp, request.email())) {
            throw new RateLimitException();
        }

        AdminUsuario admin = adminUsuarioRepository.findByEmailIgnoreCaseAndActivoTrue(request.email())
                .orElse(null);

        String hash = admin != null ? admin.getPasswordHash() : dummyPasswordHash;
        boolean coincide = passwordEncoder.matches(request.password(), hash);

        if (admin == null || !coincide) {
            rateLimiter.registrarFallo(clientIp, request.email());
            throw new BadCredentialsException("Credenciales inválidas");
        }

        rateLimiter.registrarExito(clientIp, request.email());
        return tokenService.generarToken(admin.getEmail());
    }
}
