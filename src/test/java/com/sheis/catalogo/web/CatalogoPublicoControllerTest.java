package com.sheis.catalogo.web;

import com.sheis.catalogo.service.CatalogoPublicoService;
import com.sheis.catalogo.service.ProductoPublicoService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CatalogoPublicoControllerTest {

    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Mock
    private CatalogoPublicoService catalogoPublicoService;

    @Mock
    private ProductoPublicoService productoPublicoService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CatalogoPublicoController(catalogoPublicoService, productoPublicoService, jsonMapper))
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

    @Test
    @DisplayName("GET /api/public/productos retorna productos paginados")
    void testObtenerProductosPaginados() throws Exception {
        ProductoTarjetaPublicoDto tarjeta = new ProductoTarjetaPublicoDto(
                1L,
                "audifonos-bluetooth",
                "Audífonos Bluetooth",
                "Lo más vendido",
                "/api/public/productos/1/imagenes/10",
                45000L,
                true,
                "DISPONIBLE",
                false,
                2L,
                "Audífonos",
                "audifonos"
        );
        ProductosPaginadosResponse response = new ProductosPaginadosResponse(
                List.of(tarjeta), 0, 12, 1L, 1, false
        );

        when(productoPublicoService.obtenerProductos(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(response);

        mockMvc.perform(get("/api/public/productos?categoria=audifonos&q=bluetooth&sort=relevantes&page=0&size=12")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].slug", is("audifonos-bluetooth")))
                .andExpect(jsonPath("$.content[0].nombre", is("Audífonos Bluetooth")))
                .andExpect(jsonPath("$.content[0].precioDesde", is(45000)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.hasNext", is(false)));
    }

    @Test
    @DisplayName("GET /api/public/productos/categorias retorna lista de categorías con conteo")
    void testObtenerCategoriasConConteo() throws Exception {
        List<CategoriaConteoDto> categorias = List.of(
                new CategoriaConteoDto(null, "Todos", "todos", 12),
                new CategoriaConteoDto(1L, "Audífonos", "audifonos", 4)
        );

        when(productoPublicoService.obtenerCategoriasConConteo()).thenReturn(categorias);

        mockMvc.perform(get("/api/public/productos/categorias").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nombre", is("Todos")))
                .andExpect(jsonPath("$[0].total", is(12)))
                .andExpect(jsonPath("$[1].nombre", is("Audífonos")))
                .andExpect(jsonPath("$[1].total", is(4)));
    }

    @Test
    @DisplayName("GET /api/public/productos/{idOrSlug} retorna detalle de producto")
    void testObtenerDetalleProducto() throws Exception {
        VariantePublicoDto variante = new VariantePublicoDto(
                100L, "Negro", 45000L, 5
        );
        ProductoPublicoDto detalle = new ProductoPublicoDto(
                1L,
                "Audífonos Bluetooth",
                2L,
                "Lo más vendido",
                "Descripción de audífonos",
                List.of("Estuche de carga", "Cable USB-C"),
                "Garantía de 30 días",
                "Color",
                List.of("/api/public/productos/1/imagenes/1"),
                List.of(variante)
        );

        when(productoPublicoService.obtenerDetalleProducto("audifonos-bluetooth")).thenReturn(detalle);

        mockMvc.perform(get("/api/public/productos/audifonos-bluetooth").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.nombre", is("Audífonos Bluetooth")))
                .andExpect(jsonPath("$.categoriaId", is(2)))
                .andExpect(jsonPath("$.variantes", hasSize(1)))
                .andExpect(jsonPath("$.variantes[0].nombre", is("Negro")));
    }
}
