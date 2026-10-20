package com.sheis.catalogo.web;

import com.sheis.catalogo.service.CatalogoAdminService;
import com.sheis.catalogo.service.ImagenMetodoPagoStore;
import com.sheis.catalogo.service.ImagenServicioStore;
import com.sheis.catalogo.web.dto.admin.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sheis.catalogo.exception.BadRequestException;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import com.sheis.catalogo.web.dto.publico.FavoritoDto;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración", description = "Endpoints de administración protegidos con JWT (rol ADMIN)")
@SecurityRequirement(name = "BearerAuth")
public class CatalogoAdminController {

    private final CatalogoAdminService catalogoAdminService;

    public CatalogoAdminController(CatalogoAdminService catalogoAdminService) {
        this.catalogoAdminService = catalogoAdminService;
    }

    @GetMapping("/catalogo")
    @Operation(summary = "Snapshot completo de administración", description = "Retorna todos los servicios (visibles y ocultos), planes, combos, descuentos y ajustes")
    public ResponseEntity<AdminCatalogoSnapshot> obtenerSnapshot() {
        return ResponseEntity.ok(catalogoAdminService.obtenerSnapshot());
    }

    // ==================== SERVICIOS ====================

    @PostMapping("/servicios")
    @Operation(summary = "Crear nuevo servicio", description = "Crea un servicio con su plan inicial inactivo y genera slug si falta código")
    public ResponseEntity<AdminServicioDto> crearServicio(@Valid @RequestBody CrearServicioRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoAdminService.crearServicio(req));
    }

    @PutMapping("/servicios/{codigo}")
    @Operation(summary = "Actualizar servicio existente", description = "Modifica los metadatos editables de un servicio (su código permanece inmutable)")
    public ResponseEntity<AdminServicioDto> actualizarServicio(
            @PathVariable String codigo,
            @Valid @RequestBody ActualizarServicioRequest req) {
        return ResponseEntity.ok(catalogoAdminService.actualizarServicio(codigo, req));
    }

    @PatchMapping("/servicios/{codigo}/visible")
    @Operation(summary = "Cambiar visibilidad de un servicio", description = "Oculta o muestra el servicio en el catálogo público")
    public ResponseEntity<Void> cambiarVisible(
            @PathVariable String codigo,
            @Valid @RequestBody CambiarVisibleRequest req) {
        catalogoAdminService.cambiarVisible(codigo, req.visible());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/servicios/{codigo}/stock")
    @Operation(summary = "Cambiar stock manual de un servicio", description = "Define el stock manual (null=disponible, 0=agotado, N=quedan N)")
    public ResponseEntity<Void> cambiarStock(
            @PathVariable String codigo,
            @Valid @RequestBody CambiarStockRequest req) {
        catalogoAdminService.cambiarStock(codigo, req.stock());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/servicios/{codigo}")
    @Operation(summary = "Eliminar servicio", description = "Elimina un servicio y ajusta los combos que lo contenían")
    public ResponseEntity<EliminarServicioResponse> eliminarServicio(@PathVariable String codigo) {
        return ResponseEntity.ok(catalogoAdminService.eliminarServicio(codigo));
    }

    @PutMapping(value = "/servicios/{codigo}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir o actualizar imagen del servicio", description = "Sube o actualiza la imagen PNG, JPG o WebP de un servicio (máximo 512 KB)")
    public ResponseEntity<SubirImagenResponse> subirImagen(
            @PathVariable String codigo,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            @RequestParam(value = "file", required = false) MultipartFile fileFallback
    ) throws IOException {
        MultipartFile file = archivo != null ? archivo : fileFallback;
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No se proporcionó ningún archivo de imagen");
        }
        ImagenServicioStore.ImagenGuardada guardada = catalogoAdminService.guardarImagen(
                codigo,
                file.getBytes(),
                file.getContentType()
        );
        return ResponseEntity.ok(new SubirImagenResponse(guardada.imagenUrl()));
    }

    @DeleteMapping("/servicios/{codigo}/imagen")
    @Operation(summary = "Eliminar imagen del servicio", description = "Elimina la imagen personalizada del servicio")
    public ResponseEntity<Void> eliminarImagen(@PathVariable String codigo) {
        catalogoAdminService.eliminarImagen(codigo);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/metodos-pago/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir o actualizar imagen del método de pago", description = "Sube o actualiza la imagen PNG, JPG o WebP de un método de pago (máximo 512 KB)")
    public ResponseEntity<SubirImagenResponse> subirImagenMetodoPago(
            @PathVariable String id,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            @RequestParam(value = "file", required = false) MultipartFile fileFallback
    ) throws IOException {
        MultipartFile file = archivo != null ? archivo : fileFallback;
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No se proporcionó ningún archivo de imagen");
        }
        ImagenMetodoPagoStore.ImagenGuardada guardada = catalogoAdminService.guardarImagenMetodoPago(
                id,
                file.getBytes(),
                file.getContentType()
        );
        return ResponseEntity.ok(new SubirImagenResponse(guardada.imagenUrl()));
    }

    @DeleteMapping("/metodos-pago/{id}/imagen")
    @Operation(summary = "Eliminar imagen del método de pago", description = "Elimina la imagen personalizada del método de pago")
    public ResponseEntity<Void> eliminarImagenMetodoPago(@PathVariable String id) {
        catalogoAdminService.eliminarImagenMetodoPago(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== PLANES ====================

    @GetMapping("/servicios/{codigo}/planes")
    @Operation(summary = "Obtener planes de un servicio", description = "Lista todas las tarifas activas e inactivas del servicio")
    public ResponseEntity<List<AdminPlanDto>> obtenerPlanes(@PathVariable String codigo) {
        return ResponseEntity.ok(catalogoAdminService.obtenerPlanes(codigo));
    }

    @PutMapping("/servicios/{codigo}/planes")
    @Operation(summary = "Reemplazar planes de un servicio", description = "Reemplaza completamente la lista de planes del servicio de forma transaccional")
    public ResponseEntity<List<AdminPlanDto>> reemplazarPlanes(
            @PathVariable String codigo,
            @Valid @RequestBody List<AdminPlanDto> planes) {
        return ResponseEntity.ok(catalogoAdminService.reemplazarPlanes(codigo, planes));
    }

    // ==================== COMBOS ====================

    @GetMapping("/combos")
    @Operation(summary = "Obtener lista de combos en administración")
    public ResponseEntity<List<AdminComboDto>> obtenerCombos() {
        return ResponseEntity.ok(catalogoAdminService.obtenerCombos());
    }

    @PutMapping("/combos")
    @Operation(summary = "Reemplazar lista de combos", description = "Reemplaza completamente la lista de combos predefinidos")
    public ResponseEntity<List<AdminComboDto>> reemplazarCombos(@Valid @RequestBody List<AdminComboInputDto> combos) {
        return ResponseEntity.ok(catalogoAdminService.reemplazarCombos(combos));
    }

    // ==================== DESCUENTOS ====================

    @GetMapping("/descuentos")
    @Operation(summary = "Obtener configuración de descuentos por volumen")
    public ResponseEntity<AdminDescuentosDto> obtenerDescuentos() {
        return ResponseEntity.ok(catalogoAdminService.obtenerDescuentos());
    }

    @PutMapping("/descuentos")
    @Operation(summary = "Actualizar configuración de descuentos por volumen")
    public ResponseEntity<AdminDescuentosDto> actualizarDescuentos(@Valid @RequestBody AdminDescuentosDto dto) {
        return ResponseEntity.ok(catalogoAdminService.actualizarDescuentos(dto));
    }

    // ==================== AJUSTES ====================

    @GetMapping("/ajustes")
    @Operation(summary = "Obtener ajustes generales", description = "WhatsApp, sobre nosotros, horario y métodos de pago")
    public ResponseEntity<AdminAjustesDto> obtenerAjustes() {
        return ResponseEntity.ok(catalogoAdminService.obtenerAjustes());
    }

    @PutMapping("/ajustes")
    @Operation(summary = "Actualizar ajustes generales")
    public ResponseEntity<AdminAjustesDto> actualizarAjustes(@Valid @RequestBody AdminAjustesDto dto) {
        return ResponseEntity.ok(catalogoAdminService.actualizarAjustes(dto));
    }

    // ==================== FAVORITOS ====================

    @PutMapping("/favoritos")
    @Operation(summary = "Actualizar lista de favoritos ordenados", description = "Reasigna favorito_orden a servicios y combos en una sola transacción")
    public ResponseEntity<List<FavoritoDto>> actualizarFavoritos(@Valid @RequestBody List<FavoritoItemRequest> favoritos) {
        return ResponseEntity.ok(catalogoAdminService.actualizarFavoritos(favoritos));
    }
}
