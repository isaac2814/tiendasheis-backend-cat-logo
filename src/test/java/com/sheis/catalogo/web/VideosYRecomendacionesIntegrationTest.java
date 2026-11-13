package com.sheis.catalogo.web;

import com.sheis.catalogo.domain.*;
import com.sheis.catalogo.repository.*;
import com.sheis.catalogo.service.media.InMemoryMediaStorage;
import com.sheis.catalogo.service.media.MediaService;
import com.sheis.catalogo.service.media.ProductoVideoService;
import com.sheis.catalogo.service.media.RecomendacionService;
import com.sheis.catalogo.web.dto.admin.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
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
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class VideosYRecomendacionesIntegrationTest {

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
    private CategoriaProductoRepository categoriaProductoRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ProductoVarianteRepository productoVarianteRepository;

    @Autowired
    private ProductoVideoRepository productoVideoRepository;

    @Autowired
    private RecomendacionRepository recomendacionRepository;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private ProductoVideoService productoVideoService;

    @Autowired
    private RecomendacionService recomendacionService;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private String adminToken;
    private Producto productoPrueba;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        adminUsuarioRepository.findByEmailIgnoreCaseAndActivoTrue("admin-video@test.com")
                .orElseGet(() -> adminUsuarioRepository.save(
                        AdminUsuario.builder()
                                .email("admin-video@test.com")
                                .passwordHash(passwordEncoder.encode("Secret123!"))
                                .nombre("Admin Video")
                                .activo(true)
                                .build()
                ));

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("tiendasheis-catalogo")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .subject("admin-video@test.com")
                .claim("roles", List.of("ADMIN"))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        adminToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        // Limpiar recomendaciones y videos de prueba
        recomendacionRepository.deleteAll();
        productoVideoRepository.deleteAll();

        CategoriaProducto cat = categoriaProductoRepository.findByNombreIgnoreCase("Audio Pro")
                .orElseGet(() -> categoriaProductoRepository.save(new CategoriaProducto("Audio Pro", 1, true)));

        productoPrueba = productoRepository.findAll().stream()
                .filter(p -> "Audífonos Studio".equals(p.getNombre()))
                .findFirst()
                .orElseGet(() -> {
                    Producto p = productoRepository.save(new Producto(
                            cat,
                            "Audífonos Studio",
                            "Lema sonido",
                            "Descripción detallada",
                            "Estuche, Cable",
                            "6 meses",
                            "Color",
                            1,
                            true
                    ));
                    productoVarianteRepository.save(new ProductoVariante(
                            p,
                            "Negro",
                            "AUD-NEG",
                            BigDecimal.valueOf(50000),
                            10,
                            true,
                            1
                    ));
                    return p;
                });
    }

    @Test
    @DisplayName("GET /api/admin/media/estado retorna el estado del storage")
    void testEstadoMedia() throws Exception {
        mockMvc.perform(get("/api/admin/media/estado")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").exists());
    }

    @Test
    @DisplayName("Endpoints protegidos rechazan peticiones sin autenticación con 401")
    void testEndpointsRequierenAutenticacion() throws Exception {
        mockMvc.perform(get("/api/admin/media/estado"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/recomendaciones"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Flujo de recomendación YouTube: crear, listar, activar y consultar en endpoint público")
    void testFlujoRecomendacionYouTube() throws Exception {
        CrearRecomendacionRequest crearReq = new CrearRecomendacionRequest(
                "Cómo conectar audífonos Bluetooth",
                "Guía rápida de sincronización",
                TipoRecomendacion.YOUTUBE,
                null,
                null,
                "https://youtu.be/dQw4w9WgXcQ",
                25,
                productoPrueba.getId(),
                "audio-pro",
                1,
                true
        );

        String responseBody = mockMvc.perform(post("/api/admin/recomendaciones")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsBytes(crearReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.youtubeId").value("dQw4w9WgXcQ"))
                .andExpect(jsonPath("$.activa").value(true))
                .andReturn().getResponse().getContentAsString();

        AdminRecomendacionDto creado = jsonMapper.readValue(responseBody, AdminRecomendacionDto.class);

        // Consultar endpoint público
        mockMvc.perform(get("/api/public/recomendaciones")
                        .param("categoria", "audio-pro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].youtubeId").value("dQw4w9WgXcQ"));

        // Desactivar
        ActivarRecomendacionRequest activarReq = new ActivarRecomendacionRequest(false);
        mockMvc.perform(patch("/api/admin/recomendaciones/" + creado.id() + "/activar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsBytes(activarReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(false));

        // En público ya no debe aparecer
        mockMvc.perform(get("/api/public/recomendaciones")
                        .param("categoria", "audio-pro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }
}
