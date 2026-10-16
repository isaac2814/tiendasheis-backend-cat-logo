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

    public AuthService(AdminUsuarioRepository adminUsuarioRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       LoginRateLimiter rateLimiter) {
        this.adminUsuarioRepository = adminUsuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.rateLimiter = rateLimiter;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request, String clientIp) {
        if (rateLimiter.estaBloqueado(clientIp, request.email())) {
            throw new RateLimitException("Demasiados intentos fallidos. Por favor espere 15 minutos antes de intentar de nuevo.");
        }

        AdminUsuario admin = adminUsuarioRepository.findByEmailIgnoreCaseAndActivoTrue(request.email())
                .orElse(null);

        if (admin == null || !passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            rateLimiter.registrarFallo(clientIp, request.email());
            throw new BadCredentialsException("Credenciales inválidas");
        }

        rateLimiter.registrarExito(clientIp, request.email());
        return tokenService.generarToken(admin.getEmail());
    }
}
