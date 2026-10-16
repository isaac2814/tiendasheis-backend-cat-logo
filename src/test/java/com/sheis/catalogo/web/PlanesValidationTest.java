package com.sheis.catalogo.web;

import com.sheis.catalogo.domain.SkuCategoria;
import com.sheis.catalogo.exception.BadRequestException;
import com.sheis.catalogo.repository.SkuCategoriaRepository;
import com.sheis.catalogo.repository.TarifaRepository;
import com.sheis.catalogo.service.CatalogoAdminService;
import com.sheis.catalogo.web.dto.admin.AdminPlanDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanesValidationTest {

    @Mock
    private SkuCategoriaRepository skuCategoriaRepository;

    @Mock
    private TarifaRepository tarifaRepository;

    @InjectMocks
    private CatalogoAdminService catalogoAdminService;

    @BeforeEach
    void setUp() {
        when(skuCategoriaRepository.findByCodigo("nc"))
                .thenReturn(Optional.of(SkuCategoria.builder().codigo("nc").nombre("Netflix").build()));
    }

    @Test
    @DisplayName("Validación PUT planes: combinaciones duplicadas de días y perfiles lanzan BadRequestException")
    void testPlanesDuplicados() {
        List<AdminPlanDto> planesConDuplicados = List.of(
                new AdminPlanDto(30, 1, new BigDecimal("14400"), true),
                new AdminPlanDto(30, 1, new BigDecimal("15000"), false)
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                catalogoAdminService.reemplazarPlanes("nc", planesConDuplicados)
        );

        assertTrue(ex.getMessage().contains("duplicadas"));
    }

    @Test
    @DisplayName("Validación PUT planes: precio negativo lanza BadRequestException")
    void testPlanPrecioNegativo() {
        List<AdminPlanDto> planes = List.of(
                new AdminPlanDto(30, 1, new BigDecimal("-100"), false)
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                catalogoAdminService.reemplazarPlanes("nc", planes)
        );

        assertTrue(ex.getMessage().contains("mayor o igual a 0"));
    }

    @Test
    @DisplayName("Validación PUT planes: plan activo con precio 0 lanza BadRequestException")
    void testPlanActivoPrecioCero() {
        List<AdminPlanDto> planes = List.of(
                new AdminPlanDto(30, 1, BigDecimal.ZERO, true)
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                catalogoAdminService.reemplazarPlanes("nc", planes)
        );

        assertTrue(ex.getMessage().contains("mayor a 0"));
    }

    @Test
    @DisplayName("Validación PUT planes: días o perfiles menores o iguales a cero lanzan BadRequestException")
    void testPlanDiasOPerfilesInvalidos() {
        List<AdminPlanDto> diasCero = List.of(
                new AdminPlanDto(0, 1, new BigDecimal("10000"), false)
        );
        assertThrows(BadRequestException.class, () -> catalogoAdminService.reemplazarPlanes("nc", diasCero));

        List<AdminPlanDto> perfilesCero = List.of(
                new AdminPlanDto(30, 0, new BigDecimal("10000"), false)
        );
        assertThrows(BadRequestException.class, () -> catalogoAdminService.reemplazarPlanes("nc", perfilesCero));
    }
}
