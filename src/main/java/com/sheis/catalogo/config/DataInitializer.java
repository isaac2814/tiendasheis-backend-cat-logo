package com.sheis.catalogo.config;

import com.sheis.catalogo.domain.AdminUsuario;
import com.sheis.catalogo.repository.AdminUsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

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
        // El admin inicial SOLO se crea si no hay ningún usuario. Si ya existe, nunca se
        // sobrescribe su contraseña (ni aunque cambien ADMIN_EMAIL/ADMIN_PASSWORD).
        if (adminUsuarioRepository.count() > 0) {
            return;
        }

        // AppProperties.validate() ya garantiza que email/password están presentes y son fuertes.
        String email = appProperties.getAdmin().getEmail().trim().toLowerCase();
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
}
