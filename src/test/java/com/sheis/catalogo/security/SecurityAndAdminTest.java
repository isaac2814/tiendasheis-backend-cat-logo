package com.sheis.catalogo.security;

import com.sheis.catalogo.domain.AdminUsuario;
import com.sheis.catalogo.repository.AdminUsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SecurityAndAdminTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private AdminUsuarioRepository adminUsuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        adminUsuarioRepository.findByEmailIgnoreCaseAndActivoTrue("admin@test.com")
                .ifPresentOrElse(
                        admin -> {
                            admin.setPasswordHash(passwordEncoder.encode("AdminPasswordTest123!"));
                            adminUsuarioRepository.save(admin);
                        },
                        () -> adminUsuarioRepository.save(
                                AdminUsuario.builder()
                                        .email("admin@test.com")
                                        .passwordHash(passwordEncoder.encode("AdminPasswordTest123!"))
                                        .nombre("Admin Test")
                                        .activo(true)
                                        .build()
                        )
                );
    }

    private String generarTokenConRoles(List<String> roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("tiendasheis-catalogo")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .subject("admin@test.com")
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Test
    @DisplayName("/api/admin/** sin token retorna 401 Unauthorized")
    void testAdminSinTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/admin/catalogo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/api/admin/** con token sin rol ADMIN retorna 403 Forbidden")
    void testAdminConTokenSinRolAdminRetorna403() throws Exception {
        String tokenSinAdmin = generarTokenConRoles(List.of("USER"));

        mockMvc.perform(get("/api/admin/catalogo")
                        .header("Authorization", "Bearer " + tokenSinAdmin))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("/api/admin/** con token con rol ADMIN retorna 200 OK")
    void testAdminConTokenAdminValidoRetorna200() throws Exception {
        String tokenAdmin = generarTokenConRoles(List.of("ADMIN"));

        mockMvc.perform(get("/api/admin/catalogo")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/auth/login con credenciales correctas retorna 200 y token Bearer")
    void testLoginCredencialesCorrectasRetorna200YToken() throws Exception {
        String loginJson = "{\"email\":\"admin@test.com\",\"password\":\"AdminPasswordTest123!\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEnSegundos").value(28800));
    }

    @Test
    @DisplayName("POST /api/auth/login con credenciales incorrectas retorna 401")
    void testLoginCredencialesIncorrectasRetorna401() throws Exception {
        String loginJson = "{\"email\":\"admin@test.com\",\"password\":\"clave_incorrecta_999\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El rate limiter ignora X-Forwarded-For: falsificar la IP no evade el bloqueo")
    void testRateLimiterNoSeEludeConXForwardedFor() throws Exception {
        String loginJson = "{\"email\":\"bruteforce@test.com\",\"password\":\"clave_mala\"}";
        String ipReal = "203.0.113.7";

        // 5 intentos fallidos desde la misma IP real, cada uno con un X-Forwarded-For distinto.
        for (int i = 0; i < 5; i++) {
            String xff = "10.0.0." + i;
            mockMvc.perform(post("/api/auth/login")
                            .header("X-Forwarded-For", xff)
                            .with(request -> {
                                request.setRemoteAddr(ipReal);
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isUnauthorized());
        }

        // El 6º intento debe quedar bloqueado pese a otro X-Forwarded-For diferente.
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "10.0.0.99")
                        .with(request -> {
                            request.setRemoteAddr(ipReal);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isTooManyRequests());
    }
}
