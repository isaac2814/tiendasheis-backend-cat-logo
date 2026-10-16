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
        if (adminUsuarioRepository.count() == 0) {
            String email = appProperties.getAdmin().getEmail();
            String password = appProperties.getAdmin().getPassword();

            if (email != null && !email.isBlank() && password != null && !password.isBlank()) {
                if (password.length() < 10) {
                    log.warn("ADMIN_PASSWORD configurado tiene menos de 10 caracteres; no se creará el usuario administrador por seguridad.");
                    return;
                }

                AdminUsuario admin = AdminUsuario.builder()
                        .email(email.trim().toLowerCase())
                        .passwordHash(passwordEncoder.encode(password))
                        .nombre("Administrador")
                        .activo(true)
                        .creadoEn(OffsetDateTime.now())
                        .build();

                adminUsuarioRepository.save(admin);
                log.info("Usuario administrador inicial creado exitosamente para el correo: {}", email.trim().toLowerCase());
            }
        }
    }
}
