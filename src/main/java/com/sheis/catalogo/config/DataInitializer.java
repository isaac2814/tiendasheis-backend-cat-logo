package com.sheis.catalogo.config;

import com.sheis.catalogo.domain.AdminUsuario;
import com.sheis.catalogo.repository.AdminUsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Locale;

@Component
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final AdminUsuarioRepository adminUsuarioRepository;
    private final AppProperties appProperties;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AdminUsuarioRepository adminUsuarioRepository,
                           AppProperties appProperties,
                           PasswordEncoder passwordEncoder) {
        this.adminUsuarioRepository = adminUsuarioRepository;
        this.appProperties = appProperties;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Restablecimiento opcional: se activa solo con ADMIN_RESET_PASSWORD=true.
        // AppProperties.validate() garantiza email/password presentes y fuertes.
        if (appProperties.getAdmin().isResetPasswordEnabled()) {
            restablecerPasswordAdmin();
            return;
        }

        // El admin inicial SOLO se crea si no hay ningún usuario. Si ya existe, nunca se
        // sobrescribe su contraseña (ni aunque cambien ADMIN_EMAIL/ADMIN_PASSWORD).
        if (adminUsuarioRepository.count() > 0) {
            return;
        }

        String email = appProperties.getAdmin().getEmail().trim().toLowerCase(Locale.ROOT);
        String password = appProperties.getAdmin().getPassword();

        AdminUsuario admin = AdminUsuario.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .nombre("Administrador")
                .activo(true)
                .creadoEn(OffsetDateTime.now())
                .build();

        adminUsuarioRepository.save(admin);
        log.info("Usuario administrador inicial creado para: {}", email);
    }

    private void restablecerPasswordAdmin() {
        String email = appProperties.getAdmin().getEmail().trim().toLowerCase(Locale.ROOT);
        String password = appProperties.getAdmin().getPassword();

        AdminUsuario admin = adminUsuarioRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> AdminUsuario.builder()
                        .email(email)
                        .nombre("Administrador")
                        .activo(true)
                        .creadoEn(OffsetDateTime.now())
                        .build());

        admin.setPasswordHash(passwordEncoder.encode(password));
        adminUsuarioRepository.save(admin);

        log.info("Contraseña del admin restablecida; elimina ADMIN_RESET_PASSWORD");
    }
}
