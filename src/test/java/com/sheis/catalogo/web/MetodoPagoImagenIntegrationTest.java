package com.sheis.catalogo.web;

import com.sheis.catalogo.domain.AdminUsuario;
import com.sheis.catalogo.domain.MetodoPago;
import com.sheis.catalogo.repository.AdminUsuarioRepository;
import com.sheis.catalogo.repository.MetodoPagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class MetodoPagoImagenIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private AdminUsuarioRepository adminUsuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MetodoPagoRepository metodoPagoRepository;

    private static final byte[] VALID_PNG_BYTES = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 'I', 'H', 'D', 'R',
            0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00
    };

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        adminUsuarioRepository.findByEmailIgnoreCaseAndActivoTrue("admin@test.com")
                .orElseGet(() -> adminUsuarioRepository.save(
                        AdminUsuario.builder()
                                .email("admin@test.com")
                                .passwordHash(passwordEncoder.encode("AdminPasswordTest123!"))
                                .nombre("Admin Test")
                                .activo(true)
                                .build()
                ));

        if (!metodoPagoRepository.existsById("nequi")) {
            metodoPagoRepository.save(
                    MetodoPago.builder()
                            .id("nequi")
                            .nombre("Nequi")
                            .inicial("N")
                            .color("#6A2C91")
                            .etiquetaDato("Número de Nequi")
                            .dato("3167846813")
                            .titular("Titular")
                            .activo(true)
                            .orden(1)
                            .build()
            );
        }
    }

    private String generarAdminToken() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("tiendasheis-catalogo")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .subject("admin@test.com")
                .claim("roles", List.of("ADMIN"))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Test
    @DisplayName("PUT y DELETE /api/admin/metodos-pago/{id}/imagen sin token retornan 401")
    void testEndpointsSinTokenRetornan401() throws Exception {
        MockMultipartFile file = new MockMultipartFile("archivo", "test.png", "image/png", VALID_PNG_BYTES);

        mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.PUT, "/api/admin/metodos-pago/nequi/imagen")
                        .file(file))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/admin/metodos-pago/nequi/imagen"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Subir PNG → 200, catálogo trae imagenUrl y GET público devuelve los mismos bytes")
    void testSubirPngValidoYConsultar() throws Exception {
        String token = generarAdminToken();
        MockMultipartFile file = new MockMultipartFile("archivo", "test.png", "image/png", VALID_PNG_BYTES);

        mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.PUT, "/api/admin/metodos-pago/nequi/imagen")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagenUrl", startsWith("/api/public/metodos-pago/nequi/imagen?v=")));

        mockMvc.perform(get("/api/public/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metodosPago[?(@.id == 'nequi')].imagenUrl", notNullValue()));

        mockMvc.perform(get("/api/public/metodos-pago/nequi/imagen"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(VALID_PNG_BYTES));

        mockMvc.perform(delete("/api/admin/metodos-pago/nequi/imagen")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/public/metodos-pago/nequi/imagen"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Método inexistente → 404 y archivo inválido → 400")
    void testErrores() throws Exception {
        String token = generarAdminToken();

        MockMultipartFile file = new MockMultipartFile("archivo", "test.png", "image/png", VALID_PNG_BYTES);
        mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.PUT, "/api/admin/metodos-pago/no-existe/imagen")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        byte[] fakeBytes = "No soy una imagen".getBytes();
        MockMultipartFile fakeFile = new MockMultipartFile("archivo", "fake.png", "image/png", fakeBytes);
        mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.PUT, "/api/admin/metodos-pago/nequi/imagen")
                        .file(fakeFile)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("El archivo no es una imagen válida")));
    }
}
