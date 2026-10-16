package com.sheis.catalogo.web;

import com.sheis.catalogo.exception.ResourceNotFoundException;
import com.sheis.catalogo.service.CatalogoPublicoService;
import com.sheis.catalogo.service.ImagenMetodoPagoStore;
import com.sheis.catalogo.service.ImagenServicioStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Catálogo Público", description = "Endpoints de acceso público sin autenticación para clientes")
public class CatalogoPublicoController {

    /**
     * El navegador puede reutilizar la respuesta 30 s y, pasado ese tiempo, debe revalidar con el
     * ETag (respuesta 304 barata) antes de servir una copia vieja.
     */
    private static final CacheControl CATALOGO_CACHE_CONTROL =
            CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic().mustRevalidate();

    private final CatalogoPublicoService catalogoPublicoService;
    private final JsonMapper jsonMapper;

    public CatalogoPublicoController(CatalogoPublicoService catalogoPublicoService, JsonMapper jsonMapper) {
        this.catalogoPublicoService = catalogoPublicoService;
        this.jsonMapper = jsonMapper;
    }

    @GetMapping("/catalogo")
    @Operation(summary = "Obtiene el catálogo público completo", description = "Retorna servicios visibles con sus planes activos, combos, descuentos, métodos de pago y configuración. Incluye ETag y Cache-Control para permitir respuestas 304 condicionales.")
    public ResponseEntity<byte[]> obtenerCatalogo(WebRequest request) {
        byte[] json = jsonMapper.writeValueAsBytes(catalogoPublicoService.obtenerCatalogoPublico());
        String etag = DigestUtils.md5DigestAsHex(json);

        if (request.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .cacheControl(CATALOGO_CACHE_CONTROL)
                    .build();
        }

        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CATALOGO_CACHE_CONTROL)
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
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

    @GetMapping("/metodos-pago/{id}/imagen")
    @Operation(summary = "Obtiene la imagen de un método de pago", description = "Devuelve los bytes de la imagen con cabeceras de caché inmutable")
    public ResponseEntity<byte[]> obtenerImagenMetodoPago(@PathVariable String id) {
        ImagenMetodoPagoStore.ImagenData imagen = catalogoPublicoService.obtenerImagenMetodoPago(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró imagen para el método de pago: " + id));

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(imagen.contentType()))
                .body(imagen.datos());
    }
}

