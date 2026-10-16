package com.sheis.catalogo.web;

import com.sheis.catalogo.exception.ResourceNotFoundException;
import com.sheis.catalogo.service.CatalogoPublicoService;
import com.sheis.catalogo.service.ImagenServicioStore;
import com.sheis.catalogo.web.dto.publico.CatalogoPublicoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/servicios/{codigo}/imagen")
    @Operation(summary = "Obtiene la imagen de un servicio", description = "Devuelve los bytes de la imagen con cabeceras de caché inmutable")
    public ResponseEntity<byte[]> obtenerImagen(@PathVariable String codigo) {
        ImagenServicioStore.ImagenData imagen = catalogoPublicoService.obtenerImagen(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró imagen para el servicio: " + codigo));

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(imagen.contentType()))
                .body(imagen.datos());
    }
}

