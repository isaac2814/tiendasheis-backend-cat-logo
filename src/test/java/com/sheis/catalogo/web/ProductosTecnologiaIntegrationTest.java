package com.sheis.catalogo.web;

import com.sheis.catalogo.domain.AdminUsuario;
import com.sheis.catalogo.domain.CategoriaProducto;
import com.sheis.catalogo.domain.Producto;
import com.sheis.catalogo.domain.ProductoVariante;
import com.sheis.catalogo.repository.AdminUsuarioRepository;
import com.sheis.catalogo.repository.CategoriaProductoRepository;
import com.sheis.catalogo.repository.ProductoImagenRepository;
import com.sheis.catalogo.repository.ProductoRepository;
import com.sheis.catalogo.repository.ProductoVarianteRepository;
import com.sheis.catalogo.web.dto.admin.ActualizarCategoriaProductoRequest;
import com.sheis.catalogo.web.dto.admin.ActualizarProductoRequest;
import com.sheis.catalogo.web.dto.admin.AdminVarianteInputDto;
import com.sheis.catalogo.web.dto.admin.CambiarStockRequest;
import com.sheis.catalogo.web.dto.admin.CambiarVisibleRequest;
import com.sheis.catalogo.web.dto.admin.CrearCategoriaProductoRequest;
import com.sheis.catalogo.web.dto.admin.CrearProductoRequest;
import com.sheis.catalogo.web.dto.admin.ReordenarImagenesRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
class ProductosTecnologiaIntegrationTest {

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
    private ProductoImagenRepository productoImagenRepository;

    @Autowired
    private CacheManager cacheManager;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private static final byte[] VALID_PNG_BYTES = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 'I', 'H', 'D', 'R',
            0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00
    };

    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        adminUsuarioRepository.findByEmailIgnoreCaseAndActivoTrue("admin-tec@test.com")
                .ifPresentOrElse(
                        admin -> {
                            admin.setPasswordHash(passwordEncoder.encode("Password123!"));
                            adminUsuarioRepository.save(admin);
                        },
                        () -> adminUsuarioRepository.save(
                                AdminUsuario.builder()
                                        .email("admin-tec@test.com")
                                        .passwordHash(passwordEncoder.encode("Password123!"))
                                        .nombre("Admin Tec Test")
                                        .activo(true)
                                        .build()
                        )
                );

        adminToken = generarTokenAdmin("admin-tec@test.com");
    }

    private String generarTokenAdmin(String email) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("tiendasheis-catalogo")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .subject(email)
                .claim("roles", List.of("ADMIN"))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Test
    @DisplayName("Catálogo público incluye tecnología, excluye productos sin variantes activas y no expone SKU")
    void testCatalogoPublicoTecnologia() throws Exception {
        CategoriaProducto cat = categoriaProductoRepository.findByNombreIgnoreCase("Audífonos")
                .orElseGet(() -> categoriaProductoRepository.save(new CategoriaProducto("Audífonos", 1, true)));

        // Producto 1: visible con variante activa
        Producto p1 = productoRepository.save(new Producto(
                cat, "Audífonos Bluetooth Pro", "Descripción pro", "Estuche\nCable", "30 días", "Lo más vendido", "Color", 1, true
        ));
        productoVarianteRepository.save(new ProductoVariante(
                p1, "Negro", "AUD-TEST-NEG", new BigDecimal("45000"), 10, true, 1
        ));
        productoVarianteRepository.save(new ProductoVariante(
                p1, "Blanco (inactivo)", "AUD-TEST-BLA", new BigDecimal("45000"), 0, false, 2
        ));

        // Producto 2: visible pero SIN variantes activas -> NO debe salir en público
        Producto p2 = productoRepository.save(new Producto(
                cat, "Audífonos Sin Variantes Activas", "Desc", "Incluye", "Garantía", null, "Opción", 2, true
        ));
        productoVarianteRepository.save(new ProductoVariante(
                p2, "Inactivo único", "AUD-TEST-INA", new BigDecimal("50000"), 5, false, 1
        ));

        mockMvc.perform(get("/api/public/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tecnologia").exists())
                .andExpect(jsonPath("$.tecnologia.categorias").isArray())
                .andExpect(jsonPath("$.tecnologia.productos").isArray())
                .andExpect(jsonPath("$.tecnologia.productos[?(@.id == " + p1.getId() + ")]").exists())
                .andExpect(jsonPath("$.tecnologia.productos[?(@.id == " + p2.getId() + ")]").doesNotExist())
                // Validar que las variantes del producto publicado no contienen SKU
                .andExpect(jsonPath("$.tecnologia.productos[?(@.id == " + p1.getId() + ")].variantes[0].sku").doesNotExist())
                .andExpect(jsonPath("$.tecnologia.productos[?(@.id == " + p1.getId() + ")].variantes", hasSize(1)))
                .andExpect(jsonPath("$.textos.tituloTecnologia").value("Tecnología"))
                .andExpect(jsonPath("$.textos.menuTecnologia").value("Tecnología"));
    }

    @Test
    @DisplayName("Admin snapshot incluye categorías de producto y productos con SKU y variantes inactivas")
    void testAdminSnapshotTecnologia() throws Exception {
        CategoriaProducto cat = categoriaProductoRepository.findByNombreIgnoreCase("Cargadores")
                .orElseGet(() -> categoriaProductoRepository.save(new CategoriaProducto("Cargadores", 3, true)));

        Producto prod = productoRepository.save(new Producto(
                cat, "Cargador 20W Admin Test", "Desc", "Cable", "1 año", "Rápido", "Color", 1, true
        ));
        productoVarianteRepository.save(new ProductoVariante(
                prod, "Blanco", "CARG-20W-BLA", new BigDecimal("32000"), 5, false, 1
        ));

        mockMvc.perform(get("/api/admin/catalogo")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoriasProducto").isArray())
                .andExpect(jsonPath("$.productos").isArray())
                .andExpect(jsonPath("$.productos[?(@.id == " + prod.getId() + ")].variantes[0].sku").value("CARG-20W-BLA"));
    }

    @Test
    @DisplayName("CRUD de categorías: creación, nombre duplicado 409, y eliminación con productos asociados 409")
    void testCategoriasCrud() throws Exception {
        // Crear categoría
        CrearCategoriaProductoRequest req = new CrearCategoriaProductoRequest("Gamer Gear " + System.currentTimeMillis(), 10, true);
        MvcResult res = mockMvc.perform(post("/api/admin/categorias-producto")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value(req.nombre()))
                .andReturn();

        Long catId = jsonMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        // Nombre duplicado -> 409
        mockMvc.perform(post("/api/admin/categorias-producto")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());

        // Actualizar categoría
        ActualizarCategoriaProductoRequest actReq = new ActualizarCategoriaProductoRequest("Gamer Modificado " + catId, 12, true);
        mockMvc.perform(put("/api/admin/categorias-producto/" + catId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(actReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(actReq.nombre()));

        // Asignarle un producto
        Producto prod = productoRepository.save(new Producto(
                categoriaProductoRepository.findById(catId).orElseThrow(),
                "Mouse Gamer", "Desc", "Mouse", "6 meses", null, "Color", 1, false
        ));

        // Intento de eliminar con producto -> 409 Conflict
        mockMvc.perform(delete("/api/admin/categorias-producto/" + catId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isConflict());

        // Eliminar producto y luego eliminar categoría -> 204 No Content
        productoRepository.delete(prod);
        mockMvc.perform(delete("/api/admin/categorias-producto/" + catId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Validación de productos: no publicar sin variantes activas y validar variantes únicas")
    void testProductoValidaciones() throws Exception {
        CategoriaProducto cat = categoriaProductoRepository.findAll().getFirst();

        // Crear producto visible sin variantes -> 400 Bad Request
        CrearProductoRequest reqInvalido = new CrearProductoRequest(
                cat.getId(), "Producto Sin Variantes", "Desc", "Inc", "Gar", "Lema", "Opción", 1, true, List.of()
        );
        mockMvc.perform(post("/api/admin/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(reqInvalido)))
                .andExpect(status().isBadRequest());

        // Crear producto con variantes válidas
        AdminVarianteInputDto v1 = new AdminVarianteInputDto(null, "Negro", "SKU-V1-" + System.currentTimeMillis(), new BigDecimal("15000"), 4, true, 1);
        AdminVarianteInputDto v2 = new AdminVarianteInputDto(null, "Rojo", null, new BigDecimal("16000"), 2, true, 2);
        CrearProductoRequest reqValido = new CrearProductoRequest(
                cat.getId(), "Soporte Ajustable", "Desc", "Soporte", "3 meses", null, "Color", 1, true, List.of(v1, v2)
        );

        MvcResult createRes = mockMvc.perform(post("/api/admin/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(reqValido)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Soporte Ajustable"))
                .andExpect(jsonPath("$.variantes", hasSize(2)))
                .andReturn();

        Long prodId = jsonMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // Variantes duplicadas en reemplazo -> 400 Bad Request
        AdminVarianteInputDto dup1 = new AdminVarianteInputDto(null, "Azul", null, new BigDecimal("10000"), 1, true, 1);
        AdminVarianteInputDto dup2 = new AdminVarianteInputDto(null, "azul", null, new BigDecimal("10000"), 1, true, 2);
        mockMvc.perform(put("/api/admin/productos/" + prodId + "/variantes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(List.of(dup1, dup2))))
                .andExpect(status().isBadRequest());

        // Cambiar stock de variante
        Long varId = productoVarianteRepository.findByProductoIdOrderByOrdenAscIdAsc(prodId).getFirst().getId();

        mockMvc.perform(patch("/api/admin/productos/" + prodId + "/variantes/" + varId + "/stock")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new CambiarStockRequest(25))))
                .andExpect(status().isNoContent());

        ProductoVariante varActualizada = productoVarianteRepository.findById(varId).orElseThrow();
        assertThat(varActualizada.getStock()).isEqualTo(25);
    }

    @Test
    @DisplayName("Subida, reordenamiento, límite de 6 fotos y eliminación de imágenes de producto")
    void testImagenesProductoLifecycle() throws Exception {
        CategoriaProducto cat = categoriaProductoRepository.findAll().getFirst();
        Producto prod = productoRepository.save(new Producto(
                cat, "Cable USB-C Test " + System.currentTimeMillis(), "Desc", "Cable", null, null, "Largo", 1, false
        ));

        MockMultipartFile file1 = new MockMultipartFile("archivo", "test1.png", "image/png", VALID_PNG_BYTES);
        MockMultipartFile file2 = new MockMultipartFile("archivo", "test2.png", "image/png", VALID_PNG_BYTES);

        // Subir imagen 1
        MvcResult res1 = mockMvc.perform(multipart("/api/admin/productos/" + prod.getId() + "/imagenes")
                        .file(file1)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        Long img1Id = jsonMapper.readTree(res1.getResponse().getContentAsString()).get("id").asLong();

        // Subir imagen 2
        MvcResult res2 = mockMvc.perform(multipart("/api/admin/productos/" + prod.getId() + "/imagenes")
                        .file(file2)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        Long img2Id = jsonMapper.readTree(res2.getResponse().getContentAsString()).get("id").asLong();

        // Descarga pública de la imagen 1
        mockMvc.perform(get("/api/public/productos/" + prod.getId() + "/imagenes/" + img1Id))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("immutable")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().contentType(MediaType.IMAGE_PNG));

        // Reordenar imágenes (poner img2 primero)
        mockMvc.perform(put("/api/admin/productos/" + prod.getId() + "/imagenes/orden")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new ReordenarImagenesRequest(List.of(img2Id, img1Id)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(img2Id))
                .andExpect(jsonPath("$[1].id").value(img1Id));

        // Eliminar imagen 1
        mockMvc.perform(delete("/api/admin/productos/" + prod.getId() + "/imagenes/" + img1Id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Límite de 6 imágenes: ya hay 1 imagen (img2Id), agregamos 5 más para llegar a 6
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(multipart("/api/admin/productos/" + prod.getId() + "/imagenes")
                            .file(file1)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        // La 7ma debe fallar con 400 Bad Request
        mockMvc.perform(multipart("/api/admin/productos/" + prod.getId() + "/imagenes")
                        .file(file1)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }
}
