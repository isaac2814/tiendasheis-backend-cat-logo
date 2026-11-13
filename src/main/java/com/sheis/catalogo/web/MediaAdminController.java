package com.sheis.catalogo.web;

import com.sheis.catalogo.service.media.MediaService;
import com.sheis.catalogo.service.media.PresignedUpload;
import com.sheis.catalogo.service.media.ProductoVideoService;
import com.sheis.catalogo.web.dto.admin.GuardarVideoProductoRequest;
import com.sheis.catalogo.web.dto.admin.MediaEstadoResponse;
import com.sheis.catalogo.web.dto.admin.PresignRequest;
import com.sheis.catalogo.web.dto.admin.ProductoVideoDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Media & Videos", description = "Gestión de almacenamiento R2 y videos de productos")
@SecurityRequirement(name = "bearerAuth")
public class MediaAdminController {

    private final MediaService mediaService;
    private final ProductoVideoService productoVideoService;

    public MediaAdminController(MediaService mediaService, ProductoVideoService productoVideoService) {
        this.mediaService = mediaService;
        this.productoVideoService = productoVideoService;
    }

    @GetMapping("/media/estado")
    @Operation(summary = "Estado de configuración y conectividad de R2")
    public ResponseEntity<MediaEstadoResponse> obtenerEstadoMedia() {
        return ResponseEntity.ok(mediaService.getEstado());
    }

    @PostMapping("/media/presign")
    @Operation(summary = "Generar URL prefirmada PUT para subida directa a R2")
    public ResponseEntity<PresignedUpload> generarPresign(@Valid @RequestBody PresignRequest req) {
        return ResponseEntity.ok(mediaService.presign(req));
    }

    @PutMapping("/productos/{id}/video")
    @Operation(summary = "Confirmar y asociar un video a un producto físico tras verificación de storage")
    public ResponseEntity<ProductoVideoDto> guardarVideoProducto(
            @PathVariable Long id,
            @Valid @RequestBody GuardarVideoProductoRequest req
    ) {
        return ResponseEntity.ok(productoVideoService.guardarVideo(id, req));
    }

    @DeleteMapping("/productos/{id}/video")
    @Operation(summary = "Eliminar el video asociado a un producto físico")
    public ResponseEntity<Void> eliminarVideoProducto(@PathVariable Long id) {
        productoVideoService.eliminarVideo(id);
        return ResponseEntity.noContent().build();
    }
}
