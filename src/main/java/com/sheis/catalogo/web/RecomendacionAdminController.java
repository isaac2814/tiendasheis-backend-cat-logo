package com.sheis.catalogo.web;

import com.sheis.catalogo.service.media.RecomendacionService;
import com.sheis.catalogo.web.dto.admin.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/recomendaciones")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Recomendaciones", description = "Gestión de clips y videos recomendados de Sheis")
@SecurityRequirement(name = "bearerAuth")
public class RecomendacionAdminController {

    private final RecomendacionService recomendacionService;

    public RecomendacionAdminController(RecomendacionService recomendacionService) {
        this.recomendacionService = recomendacionService;
    }

    @GetMapping
    @Operation(summary = "Listar todas las recomendaciones para administración")
    public ResponseEntity<List<AdminRecomendacionDto>> listar() {
        return ResponseEntity.ok(recomendacionService.listarAdmin());
    }

    @PostMapping
    @Operation(summary = "Crear nueva recomendación (clip subido o enlace YouTube)")
    public ResponseEntity<AdminRecomendacionDto> crear(@Valid @RequestBody CrearRecomendacionRequest req) {
        AdminRecomendacionDto creado = recomendacionService.crear(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar recomendación existente")
    public ResponseEntity<AdminRecomendacionDto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarRecomendacionRequest req
    ) {
        return ResponseEntity.ok(recomendacionService.actualizar(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar recomendación y sus archivos en almacenamiento")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        recomendacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activar")
    @Operation(summary = "Activar o desactivar recomendación")
    public ResponseEntity<AdminRecomendacionDto> activar(
            @PathVariable Long id,
            @Valid @RequestBody ActivarRecomendacionRequest req
    ) {
        return ResponseEntity.ok(recomendacionService.activar(id, req.activa()));
    }

    @PutMapping("/reordenar")
    @Operation(summary = "Reordenar recomendaciones según orden de IDs suministrado")
    public ResponseEntity<Void> reordenar(@Valid @RequestBody ReordenarRecomendacionesRequest req) {
        recomendacionService.reordenar(req.ids());
        return ResponseEntity.noContent().build();
    }
}
