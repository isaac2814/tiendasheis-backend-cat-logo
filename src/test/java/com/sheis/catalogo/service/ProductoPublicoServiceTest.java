package com.sheis.catalogo.service;

import com.sheis.catalogo.domain.CategoriaProducto;
import com.sheis.catalogo.domain.Producto;
import com.sheis.catalogo.domain.ProductoVariante;
import com.sheis.catalogo.exception.ResourceNotFoundException;
import com.sheis.catalogo.repository.CategoriaProductoRepository;
import com.sheis.catalogo.repository.ProductoImagenRepository;
import com.sheis.catalogo.repository.ProductoRepository;
import com.sheis.catalogo.repository.ProductoVarianteRepository;
import com.sheis.catalogo.web.dto.publico.CategoriaConteoDto;
import com.sheis.catalogo.web.dto.publico.ProductoImagenMeta;
import com.sheis.catalogo.web.dto.publico.ProductoPublicoDto;
import com.sheis.catalogo.web.dto.publico.ProductosPaginadosResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoPublicoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ProductoVarianteRepository productoVarianteRepository;

    @Mock
    private ProductoImagenRepository productoImagenRepository;

    @Mock
    private CategoriaProductoRepository categoriaProductoRepository;

    @InjectMocks
    private ProductoPublicoService productoPublicoService;

    private CategoriaProducto catAudifonos;
    private CategoriaProducto catCargadores;
    private Producto prod1;
    private Producto prod2;
    private Producto prod3;
    private ProductoVariante var1;
    private ProductoVariante var2;
    private ProductoVariante var3;

    @BeforeEach
    void setUp() {
        catAudifonos = new CategoriaProducto("Audífonos", 1, true);
        catAudifonos.setId(10L);

        catCargadores = new CategoriaProducto("Cargadores", 2, true);
        catCargadores.setId(20L);

        prod1 = new Producto();
        prod1.setId(1L);
        prod1.setNombre("Audífonos Bluetooth Pro");
        prod1.setLema("Lo más vendido");
        prod1.setCategoria(catAudifonos);
        prod1.setVisible(true);
        prod1.setOrden(1);

        var1 = new ProductoVariante();
        var1.setId(101L);
        var1.setProducto(prod1);
        var1.setNombre("Negro");
        var1.setPrecio(new BigDecimal("45000"));
        var1.setStock(6);
        var1.setActiva(true);

        prod2 = new Producto();
        prod2.setId(2L);
        prod2.setNombre("Cargador Rápido 20W");
        prod2.setLema("Carga rápida");
        prod2.setCategoria(catCargadores);
        prod2.setVisible(true);
        prod2.setOrden(2);

        var2 = new ProductoVariante();
        var2.setId(102L);
        var2.setProducto(prod2);
        var2.setNombre("Blanco");
        var2.setPrecio(new BigDecimal("32000"));
        var2.setStock(0); // agotado
        var2.setActiva(true);

        prod3 = new Producto();
        prod3.setId(3L);
        prod3.setNombre("Audífonos de diadema");
        prod3.setLema("Para jugar");
        prod3.setCategoria(catAudifonos);
        prod3.setVisible(true);
        prod3.setOrden(3);

        var3 = new ProductoVariante();
        var3.setId(103L);
        var3.setProducto(prod3);
        var3.setNombre("Estándar");
        var3.setPrecio(new BigDecimal("68000"));
        var3.setStock(4);
        var3.setActiva(true);
    }

    @Test
    @DisplayName("obtenerProductos pagina correctamente y respeta size default 12 y clamp")
    void testObtenerProductosPaginacion() {
        when(productoRepository.findByVisibleTrueOrderByOrdenAscIdAsc())
                .thenReturn(List.of(prod1, prod2, prod3));
        when(productoVarianteRepository.findByActivaTrueOrderByOrdenAscIdAsc())
                .thenReturn(List.of(var1, var2, var3));
        when(productoImagenRepository.findAllMetasOrderByOrdenAscIdAsc())
                .thenReturn(List.of());

        ProductosPaginadosResponse resp = productoPublicoService.obtenerProductos(null, null, null, 0, 2);

        assertEquals(0, resp.page());
        assertEquals(2, resp.size());
        assertEquals(3, resp.totalElements());
        assertEquals(2, resp.totalPages());
        assertTrue(resp.hasNext());
        assertEquals(2, resp.content().size());
    }

    @Test
    @DisplayName("obtenerProductos filtra por slug de categoría")
    void testObtenerProductosFiltroCategoriaPorSlug() {
        when(productoRepository.findByVisibleTrueOrderByOrdenAscIdAsc())
                .thenReturn(List.of(prod1, prod2, prod3));
        when(productoVarianteRepository.findByActivaTrueOrderByOrdenAscIdAsc())
                .thenReturn(List.of(var1, var2, var3));
        when(productoImagenRepository.findAllMetasOrderByOrdenAscIdAsc())
                .thenReturn(List.of());

        ProductosPaginadosResponse resp = productoPublicoService.obtenerProductos("audifonos", null, null, 0, 12);

        assertEquals(2, resp.totalElements());
        assertTrue(resp.content().stream().allMatch(p -> p.categoriaSlug().equals("audifonos")));
    }

    @Test
    @DisplayName("obtenerProductos busca con q sin tildes e insensible a mayúsculas")
    void testObtenerProductosBusquedaQ() {
        when(productoRepository.findByVisibleTrueOrderByOrdenAscIdAsc())
                .thenReturn(List.of(prod1, prod2, prod3));
        when(productoVarianteRepository.findByActivaTrueOrderByOrdenAscIdAsc())
                .thenReturn(List.of(var1, var2, var3));
        when(productoImagenRepository.findAllMetasOrderByOrdenAscIdAsc())
                .thenReturn(List.of());

        // Buscamos "rapido" sin tilde cuando el producto tiene "Cargador Rápido"
        ProductosPaginadosResponse resp = productoPublicoService.obtenerProductos(null, "rapido", null, 0, 12);

        assertEquals(1, resp.totalElements());
        assertEquals(2L, resp.content().getFirst().id());
    }

    @Test
    @DisplayName("obtenerProductos ordena por precio_asc y precio_desc")
    void testObtenerProductosOrdenPrecio() {
        when(productoRepository.findByVisibleTrueOrderByOrdenAscIdAsc())
                .thenReturn(List.of(prod1, prod2, prod3));
        when(productoVarianteRepository.findByActivaTrueOrderByOrdenAscIdAsc())
                .thenReturn(List.of(var1, var2, var3));
        when(productoImagenRepository.findAllMetasOrderByOrdenAscIdAsc())
                .thenReturn(List.of());

        // precio_asc
        ProductosPaginadosResponse respAsc = productoPublicoService.obtenerProductos(null, null, "precio_asc", 0, 12);
        assertEquals(32000L, respAsc.content().get(0).precioDesde());
        assertEquals(45000L, respAsc.content().get(1).precioDesde());
        assertEquals(68000L, respAsc.content().get(2).precioDesde());

        // precio_desc
        ProductosPaginadosResponse respDesc = productoPublicoService.obtenerProductos(null, null, "precio_desc", 0, 12);
        assertEquals(68000L, respDesc.content().get(0).precioDesde());
        assertEquals(45000L, respDesc.content().get(1).precioDesde());
        assertEquals(32000L, respDesc.content().get(2).precioDesde());
    }

    @Test
    @DisplayName("obtenerProductos con sort 'relevantes' coloca productos AGOTADO al final")
    void testObtenerProductosRelevantesAgotadosAlFinal() {
        when(productoRepository.findByVisibleTrueOrderByOrdenAscIdAsc())
                .thenReturn(List.of(prod2, prod1, prod3)); // prod2 está agotado pero viene primero
        when(productoVarianteRepository.findByActivaTrueOrderByOrdenAscIdAsc())
                .thenReturn(List.of(var2, var1, var3));
        when(productoImagenRepository.findAllMetasOrderByOrdenAscIdAsc())
                .thenReturn(List.of());

        ProductosPaginadosResponse resp = productoPublicoService.obtenerProductos(null, null, "relevantes", 0, 12);

        assertEquals(3, resp.content().size());
        assertNotEquals("AGOTADO", resp.content().get(0).stockEstado());
        assertEquals("AGOTADO", resp.content().get(2).stockEstado());
    }

    @Test
    @DisplayName("obtenerCategoriasConConteo retorna 'Todos' primero y el desglose de productos activos")
    void testObtenerCategoriasConConteo() {
        when(categoriaProductoRepository.findByVisibleTrueOrderByOrdenAscIdAsc())
                .thenReturn(List.of(catAudifonos, catCargadores));
        when(productoRepository.findByVisibleTrueOrderByOrdenAscIdAsc())
                .thenReturn(List.of(prod1, prod2, prod3));
        when(productoVarianteRepository.findByActivaTrueOrderByOrdenAscIdAsc())
                .thenReturn(List.of(var1, var2, var3));

        List<CategoriaConteoDto> cats = productoPublicoService.obtenerCategoriasConConteo();

        assertEquals(3, cats.size());
        assertEquals("Todos", cats.get(0).nombre());
        assertEquals(3L, cats.get(0).total());

        assertEquals("Audífonos", cats.get(1).nombre());
        assertEquals(2L, cats.get(1).total());

        assertEquals("Cargadores", cats.get(2).nombre());
        assertEquals(1L, cats.get(2).total());
    }

    @Test
    @DisplayName("obtenerDetalleProducto por slug retorna el producto con variantes activas")
    void testObtenerDetalleProductoPorSlug() {
        when(productoRepository.findByVisibleTrueOrderByOrdenAscIdAsc())
                .thenReturn(List.of(prod1));
        when(productoVarianteRepository.findByProductoIdAndActivaTrueOrderByOrdenAscIdAsc(1L))
                .thenReturn(List.of(var1));
        when(productoImagenRepository.findByProductoIdOrderByOrdenAscIdAsc(1L))
                .thenReturn(List.of());

        ProductoPublicoDto detalle = productoPublicoService.obtenerDetalleProducto("audifonos-bluetooth-pro");

        assertNotNull(detalle);
        assertEquals(1L, detalle.id());
        assertEquals("Audífonos Bluetooth Pro", detalle.nombre());
        assertEquals(1, detalle.variantes().size());
        assertEquals("Negro", detalle.variantes().getFirst().nombre());
    }

    @Test
    @DisplayName("obtenerDetalleProducto lanza ResourceNotFoundException si no existe")
    void testObtenerDetalleProductoNoExiste() {
        when(productoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                productoPublicoService.obtenerDetalleProducto("999"));
    }
}
