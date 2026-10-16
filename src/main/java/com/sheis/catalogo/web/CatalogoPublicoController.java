package com.sheis.catalogo.web;

import com.sheis.catalogo.service.CatalogoPublicoService;
import com.sheis.catalogo.web.dto.publico.CatalogoPublicoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Catálogo Público", description = "Endpoints de acceso público sin autenticación para clientes")
public class CatalogoPublicoController {

    private final CatalogoPublicoService catalogoPublicoService;

    public CatalogoPublicoController(CatalogoPublicoService catalogoPublicoService) {
        this.catalogoPublicoService = catalogoPublicoService;
    }

    @GetMapping("/catalogo")
    @Operation(summary = "Obtiene el catálogo público completo", description = "Retorna servicios visibles con sus planes activos, combos, descuentos, métodos de pago y configuración")
    public ResponseEntity<CatalogoPublicoResponse> obtenerCatalogo() {
        return ResponseEntity.ok(catalogoPublicoService.obtenerCatalogoPublico());
    }
}
