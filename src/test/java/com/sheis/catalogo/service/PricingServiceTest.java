package com.sheis.catalogo.service;

import com.sheis.catalogo.domain.Tarifa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PricingServiceTest {

    private PricingService pricingService;
    private Map<Integer, Long> tablaDescuentos;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService();
        tablaDescuentos = Map.of(
                2, 2500L,
                3, 5000L,
                4, 7000L,
                5, 9000L
        );
    }

    @Test
    @DisplayName("Ejemplo obligatorio: Netflix Compartido (14.400) + Disney Estándar (7.500) -> 19.400")
    void testComboNetflixYDisney() {
        List<Long> precios = List.of(14400L, 7500L);
        PricingService.ComboPricing pricing = pricingService.calcularPrecioCombo(precios, tablaDescuentos, 2000L);

        assertEquals(21900L, pricing.precioNormal());
        assertEquals(2500L, pricing.descuento());
        assertEquals(19400L, pricing.precio());
    }

    @Test
    @DisplayName("Ejemplo obligatorio: 6 servicios -> descuento 9.000 + 2.000 = 11.000")
    void testDescuentoSeisServicios() {
        long descuento = pricingService.calcularDescuento(6, tablaDescuentos, 2000L);
        assertEquals(11000L, descuento);
    }

    @Test
    @DisplayName("Bordes de cantidad n = 0, 1, 2, 5, 6, 7")
    void testBordesCantidad() {
        assertEquals(0L, pricingService.calcularDescuento(0, tablaDescuentos, 2000L));
        assertEquals(0L, pricingService.calcularDescuento(1, tablaDescuentos, 2000L));
        assertEquals(2500L, pricingService.calcularDescuento(2, tablaDescuentos, 2000L));
        assertEquals(5000L, pricingService.calcularDescuento(3, tablaDescuentos, 2000L));
        assertEquals(7000L, pricingService.calcularDescuento(4, tablaDescuentos, 2000L));
        assertEquals(9000L, pricingService.calcularDescuento(5, tablaDescuentos, 2000L));
        assertEquals(11000L, pricingService.calcularDescuento(6, tablaDescuentos, 2000L));
        assertEquals(13000L, pricingService.calcularDescuento(7, tablaDescuentos, 2000L));
    }

    @Test
    @DisplayName("Descuentos faltantes en el mapa retornan 0")
    void testDescuentosFaltantes() {
        Map<Integer, Long> vacio = Map.of();
        assertEquals(0L, pricingService.calcularDescuento(2, vacio, 2000L));
        assertEquals(2000L, pricingService.calcularDescuento(6, vacio, 2000L)); // 0 + (6-5)*2000
    }

    @Test
    @DisplayName("Precio base: selecciona plan activo de 30 días y 1 perfil")
    void testPrecioBasePlan30Dias1Perfil() {
        List<Tarifa> tarifas = List.of(
                Tarifa.builder().dias(15).cantidadPerfiles(1).precio(new BigDecimal("7900")).activo(true).build(),
                Tarifa.builder().dias(30).cantidadPerfiles(1).precio(new BigDecimal("14400")).activo(true).build(),
                Tarifa.builder().dias(60).cantidadPerfiles(1).precio(new BigDecimal("26300")).activo(true).build()
        );

        Optional<Long> base = pricingService.calcularPrecioBase(tarifas);
        assertTrue(base.isPresent());
        assertEquals(14400L, base.get());
    }

    @Test
    @DisplayName("Precio base sin plan de 30 días: toma el primer plan activo (menor días, luego menor perfiles)")
    void testPrecioBaseSinPlan30Dias() {
        List<Tarifa> tarifas = List.of(
                Tarifa.builder().dias(60).cantidadPerfiles(2).precio(new BigDecimal("30000")).activo(true).build(),
                Tarifa.builder().dias(15).cantidadPerfiles(2).precio(new BigDecimal("9000")).activo(true).build(),
                Tarifa.builder().dias(15).cantidadPerfiles(1).precio(new BigDecimal("5000")).activo(true).build()
        );

        Optional<Long> base = pricingService.calcularPrecioBase(tarifas);
        assertTrue(base.isPresent());
        assertEquals(5000L, base.get());
    }

    @Test
    @DisplayName("Precio base sin planes activos retorna empty (no se publica)")
    void testPrecioBaseSinPlanesActivos() {
        List<Tarifa> inactivos = List.of(
                Tarifa.builder().dias(30).cantidadPerfiles(1).precio(new BigDecimal("14400")).activo(false).build()
        );

        assertTrue(pricingService.calcularPrecioBase(inactivos).isEmpty());
        assertTrue(pricingService.calcularPrecioBase(List.of()).isEmpty());
        assertTrue(pricingService.calcularPrecioBase(null).isEmpty());
    }

    @Test
    @DisplayName("Precio base ignora plan 30 días si está inactivo y toma el activo válido")
    void testPrecioBasePlan30DiasInactivo() {
        List<Tarifa> tarifas = List.of(
                Tarifa.builder().dias(30).cantidadPerfiles(1).precio(new BigDecimal("14400")).activo(false).build(),
                Tarifa.builder().dias(15).cantidadPerfiles(1).precio(new BigDecimal("7900")).activo(true).build()
        );

        Optional<Long> base = pricingService.calcularPrecioBase(tarifas);
        assertTrue(base.isPresent());
        assertEquals(7900L, base.get());
    }
}
