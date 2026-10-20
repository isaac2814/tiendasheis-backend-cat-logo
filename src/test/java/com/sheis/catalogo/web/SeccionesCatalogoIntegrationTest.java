package com.sheis.catalogo.web;

import com.sheis.catalogo.domain.AdminUsuario;
import com.sheis.catalogo.domain.Combo;
import com.sheis.catalogo.domain.SkuCategoria;
import com.sheis.catalogo.repository.AdminUsuarioRepository;
import com.sheis.catalogo.repository.ComboRepository;
import com.sheis.catalogo.repository.SkuCategoriaRepository;
import com.sheis.catalogo.web.dto.admin.AdminAjustesDto;
import com.sheis.catalogo.web.dto.admin.AdminCatalogoSnapshot;
import com.sheis.catalogo.web.dto.admin.AdminComboDto;
import com.sheis.catalogo.web.dto.admin.AdminComboInputDto;
import com.sheis.catalogo.web.dto.admin.FavoritoItemRequest;
import com.sheis.catalogo.web.dto.publico.CatalogoTextosDto;
import com.sheis.catalogo.web.dto.publico.FavoritoDto;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SeccionesCatalogoIntegrationTest {

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
    private ComboRepository comboRepository;

    @Autowired
    private com.sheis.catalogo.repository.ServicioRepository servicioRepository;

    @Autowired
    private com.sheis.catalogo.repository.TarifaRepository tarifaRepository;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

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
                            .orden(1)
                            .lema("El más codiciado")
                            .favoritoOrden(1)
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

        if (!skuCategoriaRepository.existsByCodigo("de")) {
            com.sheis.catalogo.domain.Servicio serv = servicioRepository.save(
                    com.sheis.catalogo.domain.Servicio.builder().nombre("Disney").build()
            );
            SkuCategoria skuDe = skuCategoriaRepository.save(
                    SkuCategoria.builder()
                            .servicio(serv)
                            .codigo("de")
                            .nombre("Disney Estándar")
                            .modalidad("Perfil")
                            .inicial("D")
                            .color("#113CCF")
                            .visible(true)
                            .orden(2)
                            .lema("El más familiar")
                            .build()
            );
            tarifaRepository.save(
                    com.sheis.catalogo.domain.Tarifa.builder()
                            .skuCategoria(skuDe)
                            .dias(30)
                            .cantidadPerfiles(1)
                            .precio(new java.math.BigDecimal("12000"))
                            .activo(true)
                            .build()
            );
        }

        if (comboRepository.count() == 0) {
            SkuCategoria skuNc = skuCategoriaRepository.findByCodigo("nc").orElseThrow();
            SkuCategoria skuDe = skuCategoriaRepository.findByCodigo("de").orElseThrow();
            comboRepository.save(
                    new Combo(null, "Combo Test", true, true, 1, 2, List.of(skuNc, skuDe))
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
    @DisplayName("GET /api/public/catalogo contiene textos, favoritos, lema en servicios y orden/visible en combos")
    void testPublicoCatalogoContieneCamposNuevos() throws Exception {
        mockMvc.perform(get("/api/public/catalogo").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.textos", notNullValue()))
                .andExpect(jsonPath("$.textos.bannerTexto", notNullValue()))
                .andExpect(jsonPath("$.textos.tituloFavoritos", is("Servicios favoritos")))
                .andExpect(jsonPath("$.textos.tituloServicios", is("Servicios")))
                .andExpect(jsonPath("$.textos.tituloCombos", is("Combos")))
                .andExpect(jsonPath("$.textos.menuFavoritos", is("Servicios favoritos")))
                .andExpect(jsonPath("$.textos.menuServicios", is("Servicios individuales")))
                .andExpect(jsonPath("$.textos.menuCombos", is("Combos con descuento")))
                .andExpect(jsonPath("$.favoritos", notNullValue()))
                .andExpect(jsonPath("$.favoritos", not(empty())))
                .andExpect(jsonPath("$.servicios[0].orden", notNullValue()))
                .andExpect(jsonPath("$.combos[0].orden", notNullValue()))
                .andExpect(jsonPath("$.combos[0].visible", notNullValue()));
    }

    @Test
    @DisplayName("PUT /api/admin/favoritos reordena y valida integridad de referencias")
    void testActualizarFavoritosValido() throws Exception {
        String token = generarAdminToken();

        List<FavoritoItemRequest> req = List.of(
                new FavoritoItemRequest("servicio", "nc")
        );

        mockMvc.perform(put("/api/admin/favoritos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tipo", is("servicio")))
                .andExpect(jsonPath("$[0].ref", is("nc")));

        // Verificar que el catálogo público refleja el nuevo favorito y la caché se invalidó
        mockMvc.perform(get("/api/public/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoritos", hasSize(1)))
                .andExpect(jsonPath("$.favoritos[0].ref", is("nc")));
    }

    @Test
    @DisplayName("PUT /api/admin/favoritos rechaza más de 12 ítems")
    void testActualizarFavoritosRechazaExceso() throws Exception {
        String token = generarAdminToken();

        List<FavoritoItemRequest> req = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            req.add(new FavoritoItemRequest("servicio", "nc" + i));
        }

        mockMvc.perform(put("/api/admin/favoritos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("12")));
    }

    @Test
    @DisplayName("PUT /api/admin/favoritos rechaza referencias duplicadas")
    void testActualizarFavoritosRechazaDuplicados() throws Exception {
        String token = generarAdminToken();

        List<FavoritoItemRequest> req = List.of(
                new FavoritoItemRequest("servicio", "nc"),
                new FavoritoItemRequest("servicio", "nc")
        );

        mockMvc.perform(put("/api/admin/favoritos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("duplicado")));
    }

    @Test
    @DisplayName("PUT /api/admin/favoritos rechaza referencias inexistentes")
    void testActualizarFavoritosRechazaInexistentes() throws Exception {
        String token = generarAdminToken();

        List<FavoritoItemRequest> reqServicioInvalido = List.of(
                new FavoritoItemRequest("servicio", "inexistente_xyz_123")
        );

        mockMvc.perform(put("/api/admin/favoritos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(reqServicioInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("no existe")));

        List<FavoritoItemRequest> reqComboInvalido = List.of(
                new FavoritoItemRequest("combo", "999999999")
        );

        mockMvc.perform(put("/api/admin/favoritos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(reqComboInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("no existe")));
    }

    @Test
    @DisplayName("PUT /api/admin/combos actualiza in-place preservando IDs y orden de favoritos")
    void testReemplazarCombosPreservaIds() throws Exception {
        String token = generarAdminToken();

        // Obtener snapshot admin para leer combo existente
        MvcResult res = mockMvc.perform(get("/api/admin/catalogo")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        AdminCatalogoSnapshot snap = jsonMapper.readValue(res.getResponse().getContentAsString(), AdminCatalogoSnapshot.class);
        assertThat(snap.combos()).isNotEmpty();
        AdminComboDto primero = snap.combos().get(0);
        Long idOriginal = primero.id();

        // Crear payload con ese combo actualizado (nuevo badge)
        AdminComboInputDto input = new AdminComboInputDto(
                idOriginal,
                "Combo Test Editado",
                true,
                primero.servicios(),
                primero.orden(),
                true,
                "Badge Especial",
                primero.favoritoOrden()
        );

        mockMvc.perform(put("/api/admin/combos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(List.of(input))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(idOriginal.intValue())))
                .andExpect(jsonPath("$[0].badge", is("Badge Especial")));

        // Validar que en DB sigue existiendo con el mismo id
        Combo dbCombo = comboRepository.findById(idOriginal).orElseThrow();
        assertThat(dbCombo.getEtiqueta()).isEqualTo("Badge Especial");
    }

    @Test
    @DisplayName("PUT /api/admin/ajustes valida límites de caracteres en textos y actualiza DB")
    void testActualizarAjustesValidaLimites() throws Exception {
        String token = generarAdminToken();

        // Leer ajustes actuales
        MvcResult res = mockMvc.perform(get("/api/admin/ajustes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        AdminAjustesDto ajustes = jsonMapper.readValue(res.getResponse().getContentAsString(), AdminAjustesDto.class);

        // 1. Probar bannerTexto excediendo 90 caracteres -> 400
        CatalogoTextosDto textosExcedidos = new CatalogoTextosDto(
                "A".repeat(91),
                ajustes.textos().tituloFavoritos(),
                ajustes.textos().tituloServicios(),
                ajustes.textos().tituloCombos(),
                ajustes.textos().menuFavoritos(),
                ajustes.textos().menuServicios(),
                ajustes.textos().menuCombos()
        );

        AdminAjustesDto ajustesExcedidos = new AdminAjustesDto(
                ajustes.whatsapp(),
                ajustes.sobreNosotros(),
                ajustes.horario(),
                ajustes.metodosPago(),
                textosExcedidos
        );

        mockMvc.perform(put("/api/admin/ajustes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(ajustesExcedidos)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("90")));

        // 2. Probar textos válidos -> 200 y reflejados en público
        CatalogoTextosDto textosValidos = new CatalogoTextosDto(
                "Ahorra hasta 25% y entrega inmediata",
                "Favoritos de hoy",
                "Todos los Servicios",
                "Todos los Combos",
                "Top",
                "Servicios",
                "Paquetes"
        );
        AdminAjustesDto ajustesValidos = new AdminAjustesDto(
                ajustes.whatsapp(),
                ajustes.sobreNosotros(),
                ajustes.horario(),
                ajustes.metodosPago(),
                textosValidos
        );

        mockMvc.perform(put("/api/admin/ajustes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(ajustesValidos)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.textos.tituloFavoritos", is("Favoritos de hoy")))
                .andExpect(jsonPath("$.textos.menuFavoritos", is("Top")));

        // Verificar catálogo público
        mockMvc.perform(get("/api/public/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.textos.tituloFavoritos", is("Favoritos de hoy")))
                .andExpect(jsonPath("$.textos.menuFavoritos", is("Top")));
    }

    @Test
    @DisplayName("GET /api/public/catalogo responde 304 con ETag y evicta la caché al haber mutación admin")
    void testEtagY304YEviccionCache() throws Exception {
        String token = generarAdminToken();

        // 1. Petición inicial obtiene 200 y cabecera ETag
        MvcResult res1 = mockMvc.perform(get("/api/public/catalogo").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        String etagOriginal = res1.getResponse().getHeader(HttpHeaders.ETAG);
        assertThat(etagOriginal).isNotBlank();

        // 2. Petición condicional con If-None-Match devuelve 304 Not Modified
        mockMvc.perform(get("/api/public/catalogo")
                        .header(HttpHeaders.IF_NONE_MATCH, etagOriginal))
                .andExpect(status().isNotModified());

        // 3. Mutación admin evicta la caché
        MvcResult resAjustes = mockMvc.perform(get("/api/admin/ajustes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        AdminAjustesDto ajustes = jsonMapper.readValue(resAjustes.getResponse().getContentAsString(), AdminAjustesDto.class);

        CatalogoTextosDto textosModificados = new CatalogoTextosDto(
                "Texto actualizado tras mutación",
                ajustes.textos().tituloFavoritos(),
                ajustes.textos().tituloServicios(),
                ajustes.textos().tituloCombos(),
                ajustes.textos().menuFavoritos(),
                ajustes.textos().menuServicios(),
                ajustes.textos().menuCombos()
        );
        AdminAjustesDto ajustesModificados = new AdminAjustesDto(
                ajustes.whatsapp(),
                ajustes.sobreNosotros(),
                ajustes.horario(),
                ajustes.metodosPago(),
                textosModificados
        );

        mockMvc.perform(put("/api/admin/ajustes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(ajustesModificados)))
                .andExpect(status().isOk());

        // 4. Con el ETag viejo, ahora debe responder 200 con nuevo ETag y nuevo contenido
        MvcResult res2 = mockMvc.perform(get("/api/public/catalogo")
                        .header(HttpHeaders.IF_NONE_MATCH, etagOriginal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.textos.bannerTexto", is("Texto actualizado tras mutación")))
                .andReturn();

        String nuevoEtag = res2.getResponse().getHeader(HttpHeaders.ETAG);
        assertThat(nuevoEtag).isNotEqualTo(etagOriginal);
    }

    @Test
    @DisplayName("PUT /api/admin/ajustes con bannerTexto vacío se almacena y devuelve vacío en catálogo público para ocultar la banda")
    void testBannerVacioPermiteOcultarBanda() throws Exception {
        String token = generarAdminToken();

        MvcResult resAjustes = mockMvc.perform(get("/api/admin/ajustes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        AdminAjustesDto ajustes = jsonMapper.readValue(resAjustes.getResponse().getContentAsString(), AdminAjustesDto.class);

        CatalogoTextosDto textosBannerVacio = new CatalogoTextosDto(
                "",
                ajustes.textos().tituloFavoritos(),
                ajustes.textos().tituloServicios(),
                ajustes.textos().tituloCombos(),
                ajustes.textos().menuFavoritos(),
                ajustes.textos().menuServicios(),
                ajustes.textos().menuCombos()
        );
        AdminAjustesDto ajustesModificados = new AdminAjustesDto(
                ajustes.whatsapp(),
                ajustes.sobreNosotros(),
                ajustes.horario(),
                ajustes.metodosPago(),
                textosBannerVacio
        );

        mockMvc.perform(put("/api/admin/ajustes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(ajustesModificados)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.textos.bannerTexto", is("")));

        mockMvc.perform(get("/api/public/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.textos.bannerTexto", is("")));
    }

    @Test
    @DisplayName("Combo con visible=false no debe incluirse en la lista pública de favoritos")
    void testComboOcultoNoApareceEnFavoritosPublicos() throws Exception {
        String token = generarAdminToken();

        // Obtener combos actuales
        MvcResult res = mockMvc.perform(get("/api/admin/catalogo")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        AdminCatalogoSnapshot snap = jsonMapper.readValue(res.getResponse().getContentAsString(), AdminCatalogoSnapshot.class);
        assertThat(snap.combos()).isNotEmpty();
        AdminComboDto comboTarget = snap.combos().get(0);

        // Ponerlo en favoritos
        List<FavoritoItemRequest> favReq = List.of(
                new FavoritoItemRequest("combo", String.valueOf(comboTarget.id()))
        );
        mockMvc.perform(put("/api/admin/favoritos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(favReq)))
                .andExpect(status().isOk());

        // Con visible=true, aparece en favoritos públicos
        mockMvc.perform(get("/api/public/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoritos", hasSize(1)))
                .andExpect(jsonPath("$.favoritos[0].ref", is(String.valueOf(comboTarget.id()))));

        // Ahora ocultar el combo (visible=false) mediante PUT /api/admin/combos
        List<AdminComboInputDto> combosOculto = snap.combos().stream()
                .map(c -> new AdminComboInputDto(
                        c.id(),
                        c.etiqueta(),
                        c.id().equals(comboTarget.id()) ? false : c.activo(),
                        c.servicios(),
                        c.orden(),
                        c.id().equals(comboTarget.id()) ? false : c.visible(),
                        c.badge(),
                        c.favoritoOrden()
                ))
                .toList();

        mockMvc.perform(put("/api/admin/combos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(combosOculto)))
                .andExpect(status().isOk());

        // En el catálogo público, el combo oculto NO debe aparecer en favoritos
        mockMvc.perform(get("/api/public/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoritos", empty()));
    }
}
