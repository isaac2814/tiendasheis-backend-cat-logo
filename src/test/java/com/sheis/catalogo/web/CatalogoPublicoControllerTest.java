package com.sheis.catalogo.web;

import com.sheis.catalogo.service.CatalogoPublicoService;
import com.sheis.catalogo.web.dto.publico.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CatalogoPublicoControllerTest {

    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Mock
    private CatalogoPublicoService catalogoPublicoService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CatalogoPublicoController(catalogoPublicoService, jsonMapper))
                .build();
    }

    private CatalogoPublicoResponse catalogoResponseDePrueba() {
        PlanDto planActivo = new PlanDto(30, 1, 14400L);
        ServicioPublicoDto servicio = new ServicioPublicoDto(
                "nc",
                "Netflix Compartido",
                "N",
                "#8B1A1A",
                null,
                "Perfil compartido",
                "Descripción de prueba",
                List.of("Perfil con PIN"),
                List.of("No cambiar contraseña"),
                null,
                false,
                14400L,
                List.of(planActivo)
        );

        ComboPublicoDto combo = new ComboPublicoDto(
                1L,
                "Popular",
                List.of("nc", "de"),
                21900L,
                2500L,
                19400L,
                false
        );

        DescuentosDto descuentos = new DescuentosDto(Map.of("2", 2500L), 2000L);
        MetodoPagoDto metodo = new MetodoPagoDto("nequi", "Nequi", "N", "#6A2C91", "Número de Nequi", "3167846813", "Titular", null);
        HorarioDto horario = new HorarioDto("Lunes a viernes", "8:00 AM - 10:00 PM");

        return new CatalogoPublicoResponse(
                List.of(servicio),
                List.of(combo),
                descuentos,
                List.of(metodo),
                List.of(horario),
                "57 300 000 0000",
                "Somos TiendaSheis"
        );
    }

    @Test
    @DisplayName("GET /api/public/catalogo retorna JSON con estructura exacta y sin servicios ocultos ni planes inactivos")
    void testObtenerCatalogoPublico() throws Exception {
        when(catalogoPublicoService.obtenerCatalogoPublico()).thenReturn(catalogoResponseDePrueba());

        mockMvc.perform(get("/api/public/catalogo").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // Verificar estructura raíz
                .andExpect(jsonPath("$.servicios", hasSize(1)))
                .andExpect(jsonPath("$.servicios[0].id", is("nc")))
                .andExpect(jsonPath("$.servicios[0].nombre", is("Netflix Compartido")))
                .andExpect(jsonPath("$.servicios[0].precioBase", is(14400)))
                .andExpect(jsonPath("$.servicios[0].planes", hasSize(1)))
                .andExpect(jsonPath("$.servicios[0].planes[0].dias", is(30)))
                .andExpect(jsonPath("$.servicios[0].planes[0].precio", is(14400)))
                // Verificar combos
                .andExpect(jsonPath("$.combos", hasSize(1)))
                .andExpect(jsonPath("$.combos[0].etiqueta", is("Popular")))
                .andExpect(jsonPath("$.combos[0].precio", is(19400)))
                // Verificar descuentos
                .andExpect(jsonPath("$.descuentos.porCantidad['2']", is(2500)))
                .andExpect(jsonPath("$.descuentos.extraDesdeSexto", is(2000)))
                // Metodos de pago y horarios
                .andExpect(jsonPath("$.metodosPago", hasSize(1)))
                .andExpect(jsonPath("$.metodosPago[0].id", is("nequi")))
                .andExpect(jsonPath("$.horario", hasSize(1)))
                .andExpect(jsonPath("$.whatsapp", is("57 300 000 0000")))
                .andExpect(jsonPath("$.sobreNosotros", is("Somos TiendaSheis")));
    }

    @Test
    @DisplayName("GET /api/public/catalogo devuelve ETag + Cache-Control y responde 304 con If-None-Match")
    void testEtagYCacheControl() throws Exception {
        when(catalogoPublicoService.obtenerCatalogoPublico()).thenReturn(catalogoResponseDePrueba());

        MvcResult primera = mockMvc.perform(get("/api/public/catalogo").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("public")))
                .andReturn();

        String etag = primera.getResponse().getHeader(HttpHeaders.ETAG);
        assertNotNull(etag);

        mockMvc.perform(get("/api/public/catalogo")
                        .header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified())
                .andExpect(header().string(HttpHeaders.ETAG, etag));
    }
}
