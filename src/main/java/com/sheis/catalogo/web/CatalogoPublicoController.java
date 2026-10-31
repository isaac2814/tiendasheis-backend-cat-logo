package com.sheis.catalogo.web;

import com.sheis.catalogo.exception.ResourceNotFoundException;
import com.sheis.catalogo.service.CatalogoPublicoService;
import com.sheis.catalogo.service.ImagenMetodoPagoStore;
import com.sheis.catalogo.service.ImagenProductoStore;
import com.sheis.catalogo.service.ImagenServicioStore;
import com.sheis.catalogo.service.ProductoPublicoService;
import com.sheis.catalogo.web.dto.publico.CategoriaConteoDto;
import com.sheis.catalogo.web.dto.publico.ProductoPublicoDto;
import com.sheis.catalogo.web.dto.publico.ProductosPaginadosResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Catálogo Público", description = "Endpoints de acceso público sin autenticación para clientes")
public class CatalogoPublicoController {

    /**
     * El navegador puede reutilizar la respuesta 60 s y, pasado ese tiempo, debe revalidar con el
     * ETag (respuesta 304 barata) antes de servir una copia vieja.
     */
    private static final CacheControl CATALOGO_CACHE_CONTROL =
            CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic().mustRevalidate();

    private final CatalogoPublicoService catalogoPublicoService;
    private final ProductoPublicoService productoPublicoService;
    private final JsonMapper jsonMapper;

    public CatalogoPublicoController(CatalogoPublicoService catalogoPublicoService,
                                   ProductoPublicoService productoPublicoService,
                                   JsonMapper jsonMapper) {
        this.catalogoPublicoService = catalogoPublicoService;
        this.productoPublicoService = productoPublicoService;
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

    @GetMapping("/productos/{productoId}/imagenes/{imagenId}")
    @Operation(summary = "Obtiene una imagen de un producto físico", description = "Devuelve los bytes de la imagen con cabeceras de caché inmutable")
    public ResponseEntity<byte[]> obtenerImagenProducto(
            @PathVariable Long productoId,
            @PathVariable Long imagenId) {
        ImagenProductoStore.ImagenData imagen = catalogoPublicoService.obtenerImagenProducto(productoId, imagenId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró imagen " + imagenId + " para el producto: " + productoId));

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(imagen.contentType()))
                .body(imagen.datos());
    }

    // ==================== PRODUCTOS TECNOLOGÍA PÚBLICOS ====================

    @GetMapping("/productos")
    @Operation(summary = "Obtiene productos tecnológicos paginados", description = "Permite filtrar por categoría (id o slug), buscar por texto (q) y ordenar por relevantes, precio_asc, precio_desc o nuevos.")
    public ResponseEntity<ProductosPaginadosResponse> obtenerProductos(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "relevantes") String sort,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "12") Integer size) {
        ProductosPaginadosResponse resp = productoPublicoService.obtenerProductos(categoria, q, sort, page, size);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/productos/categorias")
    @Operation(summary = "Obtiene las categorías de tecnología con conteo de productos", description = "Retorna categorías activas con la cantidad de productos disponibles y el total general en el primer elemento.")
    public ResponseEntity<List<CategoriaConteoDto>> obtenerCategoriasConConteo() {
        return ResponseEntity.ok(productoPublicoService.obtenerCategoriasConConteo());
    }

    @GetMapping("/productos/{idOrSlug}")
    @Operation(summary = "Obtiene el detalle de un producto por ID o Slug", description = "Retorna la ficha completa del producto con sus variantes activas, imágenes y características.")
    public ResponseEntity<ProductoPublicoDto> obtenerDetalleProducto(@PathVariable String idOrSlug) {
        return ResponseEntity.ok(productoPublicoService.obtenerDetalleProducto(idOrSlug));
    }
}

