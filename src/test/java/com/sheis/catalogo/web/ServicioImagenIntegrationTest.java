package com.sheis.catalogo.web;

import com.sheis.catalogo.domain.AdminUsuario;
import com.sheis.catalogo.domain.SkuCategoria;
import com.sheis.catalogo.repository.AdminUsuarioRepository;
import com.sheis.catalogo.repository.ServicioImagenRepository;
import com.sheis.catalogo.repository.SkuCategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ServicioImagenIntegrationTest {

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
    private SkuCategoriaRepository skuCategoriaRepository;

    @Autowired
    private ServicioImagenRepository servicioImagenRepository;

    @Autowired
    private com.sheis.catalogo.repository.ServicioRepository servicioRepository;

    @Autowired
    private com.sheis.catalogo.repository.TarifaRepository tarifaRepository;

    // Bytes válidos de cabecera PNG de prueba (89 50 4E 47 0D 0A 1A 0A + bytes)
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

        if (!skuCategoriaRepository.existsByCodigo("nc")) {
            com.sheis.catalogo.domain.Servicio serv = servicioRepository.save(
                    com.sheis.catalogo.domain.Servicio.builder().nombre("Netflix").build()
            );
            SkuCategoria sku = skuCategoriaRepository.save(
                    SkuCategoria.builder()
                            .servicio(serv)
                            .codigo("nc")
                            .nombre("Netflix Compartido")
                            .modalidad("Perfil")
                            .inicial("N")
                            .color("#8B1A1A")
                            .visible(true)
                            .build()
            );
            tarifaRepository.save(
                    com.sheis.catalogo.domain.Tarifa.builder()
                            .skuCategoria(sku)
                            .dias(30)
                            .cantidadPerfiles(1)
                            .precio(new java.math.BigDecimal("14400"))
                            .activo(true)
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
    @DisplayName("PUT y DELETE /api/admin/servicios/{codigo}/imagen sin token retornan 401")
    void testEndpointsSinTokenRetornan401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "archivo", "test.png", "image/png", VALID_PNG_BYTES
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.PUT, "/api/admin/servicios/nc/imagen")
                        .file(file))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/admin/servicios/nc/imagen"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Subir PNG válido → 200, catálogo trae imagenUrl y GET público devuelve los mismos bytes")
    void testSubirPngValidoYConsultar() throws Exception {
        String token = generarAdminToken();

        MockMultipartFile file = new MockMultipartFile(
                "archivo", "test.png", "image/png", VALID_PNG_BYTES
        );

        // 1. Subir imagen
        mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.PUT, "/api/admin/servicios/nc/imagen")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagenUrl", startsWith("/api/public/servicios/nc/imagen?v=")));

        // 2. Consultar catálogo público
        mockMvc.perform(get("/api/public/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicios[?(@.id == 'nc')].imagenUrl", notNullValue()));

        // 3. GET público de la imagen
        mockMvc.perform(get("/api/public/servicios/nc/imagen"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(VALID_PNG_BYTES));

        // 4. DELETE imagen → 204
        mockMvc.perform(delete("/api/admin/servicios/nc/imagen")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // 5. GET público después de borrar → 404
        mockMvc.perform(get("/api/public/servicios/nc/imagen"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Archivo no imagen o .txt renombrado a .png → 400")
    void testArchivoInvalidoRetorna400() throws Exception {
        String token = generarAdminToken();
        byte[] fakeBytes = "Este es un texto simple y no es una imagen".getBytes();

        MockMultipartFile fakeFile = new MockMultipartFile(
                "archivo", "fake.png", "image/png", fakeBytes
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.PUT, "/api/admin/servicios/nc/imagen")
                        .file(fakeFile)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("El archivo no es una imagen válida")));
    }

    @Test
    @DisplayName("Archivo mayor a 512 KB → 400 con mensaje claro")
    void testArchivoMayorA512KbRetorna400() throws Exception {
        String token = generarAdminToken();
        // Array de 513 KB con encabezado PNG
        byte[] largeBytes = new byte[513 * 1024];
        System.arraycopy(VALID_PNG_BYTES, 0, largeBytes, 0, VALID_PNG_BYTES.length);

        MockMultipartFile largeFile = new MockMultipartFile(
                "archivo", "large.png", "image/png", largeBytes
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.PUT, "/api/admin/servicios/nc/imagen")
                        .file(largeFile)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("512 KB")));
    }

    @Test
    @DisplayName("Subir imagen a servicio inexistente → 404")
    void testServicioInexistenteRetorna404() throws Exception {
        String token = generarAdminToken();

        MockMultipartFile file = new MockMultipartFile(
                "archivo", "test.png", "image/png", VALID_PNG_BYTES
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.PUT, "/api/admin/servicios/codigo-que-no-existe/imagen")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Borrar un servicio borra su imagen en cascada")
    @Transactional
    void testBorrarServicioBorraImagenEnCascada() {
        com.sheis.catalogo.domain.Servicio serv = servicioRepository.save(
                com.sheis.catalogo.domain.Servicio.builder().nombre("Cascade Test").build()
        );
        SkuCategoria sku = skuCategoriaRepository.save(
                SkuCategoria.builder()
                        .servicio(serv)
                        .codigo("cascade-test")
                        .nombre("Cascade Test")
                        .modalidad("Perfil")
                        .inicial("C")
                        .color("#123456")
                        .visible(true)
                        .build()
        );
        skuCategoriaRepository.flush();
        // Guardar imagen en repositorio
        com.sheis.catalogo.domain.ServicioImagen imagen = new com.sheis.catalogo.domain.ServicioImagen(
                sku, "image/png", VALID_PNG_BYTES, VALID_PNG_BYTES.length, Instant.now()
        );
        sku.setImagen(imagen);
        servicioImagenRepository.saveAndFlush(imagen);
        assertThat(servicioImagenRepository.existsById(sku.getId())).isTrue();

        // Al eliminar el servicio
        skuCategoriaRepository.delete(sku);
        skuCategoriaRepository.flush();

        // La imagen debe haberse eliminado en cascada
        assertThat(servicioImagenRepository.existsById(sku.getId())).isFalse();
    }
}
