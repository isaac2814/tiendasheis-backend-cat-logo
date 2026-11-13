package com.sheis.catalogo.service.media;

import com.sheis.catalogo.domain.Producto;
import com.sheis.catalogo.domain.Recomendacion;
import com.sheis.catalogo.domain.TipoRecomendacion;
import com.sheis.catalogo.exception.ResourceNotFoundException;
import com.sheis.catalogo.exception.ServiceUnavailableException;
import com.sheis.catalogo.exception.UnprocessableEntityException;
import com.sheis.catalogo.repository.ProductoRepository;
import com.sheis.catalogo.repository.RecomendacionRepository;
import com.sheis.catalogo.util.YouTubeUrlExtractor;
import com.sheis.catalogo.web.dto.admin.ActualizarRecomendacionRequest;
import com.sheis.catalogo.web.dto.admin.AdminRecomendacionDto;
import com.sheis.catalogo.web.dto.admin.CrearRecomendacionRequest;
import com.sheis.catalogo.web.dto.publico.PublicRecomendacionDto;
import com.sheis.catalogo.web.dto.publico.PublicRecomendacionesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class RecomendacionService {

    public static final int MAX_RECOMMENDATION_DURATION_SEC = 45;

    private final RecomendacionRepository recomendacionRepository;
    private final ProductoRepository productoRepository;
    private final MediaStorage mediaStorage;

    public RecomendacionService(
            RecomendacionRepository recomendacionRepository,
            ProductoRepository productoRepository,
            MediaStorage mediaStorage
    ) {
        this.recomendacionRepository = recomendacionRepository;
        this.productoRepository = productoRepository;
        this.mediaStorage = mediaStorage;
    }

    @Transactional(readOnly = true)
    public List<AdminRecomendacionDto> listarAdmin() {
        return recomendacionRepository.findAllByOrderByOrdenAscIdAsc().stream()
                .map(this::toAdminDto)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public AdminRecomendacionDto crear(CrearRecomendacionRequest req) {
        Producto producto = null;
        if (req.productoId() != null) {
            producto = productoRepository.findById(req.productoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID " + req.productoId()));
        }

        int orden = (req.orden() != null) ? req.orden() : calcularSiguienteOrden();
        boolean activa = (req.activa() != null) ? req.activa() : true;

        if (req.tipo() == TipoRecomendacion.CLIP) {
            if (!mediaStorage.isEnabled()) {
                throw new ServiceUnavailableException("El almacenamiento multimedia en la nube (R2) no está habilitado");
            }
            if (req.objectKey() == null || req.objectKey().isBlank()) {
                throw new UnprocessableEntityException("El video (objectKey) es obligatorio para recomendaciones de tipo CLIP");
            }
            if (req.posterKey() == null || req.posterKey().isBlank()) {
                throw new UnprocessableEntityException("La portada (posterKey) es obligatoria para recomendaciones de tipo CLIP");
            }

            int duracion = req.duracionSeg() != null ? req.duracionSeg() : 0;
            if (duracion <= 0 || duracion > MAX_RECOMMENDATION_DURATION_SEC) {
                eliminarObjetosSilencioso(req.objectKey(), req.posterKey());
                throw new UnprocessableEntityException("La duración del clip (" + duracion + "s) excede el máximo permitido de " + MAX_RECOMMENDATION_DURATION_SEC + " segundos");
            }

            Optional<MediaObjectInfo> videoInfoOpt = mediaStorage.head(req.objectKey());
            if (videoInfoOpt.isEmpty()) {
                eliminarObjetosSilencioso(req.objectKey(), req.posterKey());
                throw new UnprocessableEntityException("El video subido no se encuentra en el almacenamiento");
            }
            MediaObjectInfo videoInfo = videoInfoOpt.get();
            if (videoInfo.contentType() == null || !videoInfo.contentType().toLowerCase().contains("mp4")) {
                eliminarObjetosSilencioso(req.objectKey(), req.posterKey());
                throw new UnprocessableEntityException("El archivo de video debe ser de formato video/mp4");
            }
            if (videoInfo.sizeBytes() > MediaService.MAX_RECOMMENDATION_VIDEO_BYTES) {
                eliminarObjetosSilencioso(req.objectKey(), req.posterKey());
                throw new UnprocessableEntityException("El video excede el límite de 15 MB");
            }

            Optional<MediaObjectInfo> posterInfoOpt = mediaStorage.head(req.posterKey());
            if (posterInfoOpt.isEmpty()) {
                eliminarObjetosSilencioso(req.objectKey(), req.posterKey());
                throw new UnprocessableEntityException("La portada subida no se encuentra en el almacenamiento");
            }
            MediaObjectInfo posterInfo = posterInfoOpt.get();
            String posterCt = posterInfo.contentType() != null ? posterInfo.contentType().toLowerCase() : "";
            if (!posterCt.contains("webp") && !posterCt.contains("jpeg") && !posterCt.contains("jpg")) {
                eliminarObjetosSilencioso(req.objectKey(), req.posterKey());
                throw new UnprocessableEntityException("La portada debe ser de formato image/webp o image/jpeg");
            }
            if (posterInfo.sizeBytes() > MediaService.MAX_POSTER_BYTES) {
                eliminarObjetosSilencioso(req.objectKey(), req.posterKey());
                throw new UnprocessableEntityException("La portada excede el límite de 150 KB");
            }

            Recomendacion rec = new Recomendacion(
                    req.titulo().trim(),
                    limpiarTexto(req.descripcion()),
                    TipoRecomendacion.CLIP,
                    req.objectKey(),
                    req.posterKey(),
                    null,
                    duracion,
                    videoInfo.sizeBytes(),
                    producto,
                    limpiarTexto(req.categoriaSlug()),
                    orden,
                    activa
            );

            return toAdminDto(recomendacionRepository.save(rec));

        } else if (req.tipo() == TipoRecomendacion.YOUTUBE) {
            String youtubeId = YouTubeUrlExtractor.extractId(req.youtubeUrlOrId())
                    .orElseThrow(() -> new UnprocessableEntityException("Enlace o ID de YouTube no válido"));

            int duracion = (req.duracionSeg() != null && req.duracionSeg() > 0)
                    ? Math.min(req.duracionSeg(), MAX_RECOMMENDATION_DURATION_SEC)
                    : 0;

            Recomendacion rec = new Recomendacion(
                    req.titulo().trim(),
                    limpiarTexto(req.descripcion()),
                    TipoRecomendacion.YOUTUBE,
                    null,
                    null,
                    youtubeId,
                    duracion,
                    null,
                    producto,
                    limpiarTexto(req.categoriaSlug()),
                    orden,
                    activa
            );

            return toAdminDto(recomendacionRepository.save(rec));
        }

        throw new UnprocessableEntityException("Tipo de recomendación no válido: " + req.tipo());
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public AdminRecomendacionDto actualizar(Long id, ActualizarRecomendacionRequest req) {
        Recomendacion rec = recomendacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recomendación no encontrada con ID " + id));

        Producto producto = null;
        if (req.productoId() != null) {
            producto = productoRepository.findById(req.productoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID " + req.productoId()));
        }

        rec.setTitulo(req.titulo().trim());
        rec.setDescripcion(limpiarTexto(req.descripcion()));
        rec.setProducto(producto);
        rec.setCategoriaSlug(limpiarTexto(req.categoriaSlug()));
        if (req.orden() != null) rec.setOrden(req.orden());
        if (req.activa() != null) rec.setActiva(req.activa());

        if (req.tipo() == TipoRecomendacion.CLIP) {
            if (!mediaStorage.isEnabled()) {
                throw new ServiceUnavailableException("El almacenamiento multimedia en la nube (R2) no está habilitado");
            }
            if (req.objectKey() == null || req.objectKey().isBlank()) {
                throw new UnprocessableEntityException("El video (objectKey) es obligatorio para recomendaciones de tipo CLIP");
            }
            if (req.posterKey() == null || req.posterKey().isBlank()) {
                throw new UnprocessableEntityException("La portada (posterKey) es obligatoria para recomendaciones de tipo CLIP");
            }

            int duracion = req.duracionSeg() != null ? req.duracionSeg() : rec.getDuracionSeg();
            if (duracion <= 0 || duracion > MAX_RECOMMENDATION_DURATION_SEC) {
                if (!req.objectKey().equals(rec.getObjectKey()) || !req.posterKey().equals(rec.getPosterKey())) {
                    eliminarObjetosSilencioso(req.objectKey(), req.posterKey());
                }
                throw new UnprocessableEntityException("La duración del clip (" + duracion + "s) excede el máximo permitido de " + MAX_RECOMMENDATION_DURATION_SEC + " segundos");
            }

            // Si cambió el video, validar el nuevo
            long sizeBytes = rec.getTamanoBytes() != null ? rec.getTamanoBytes() : 0L;
            if (!req.objectKey().equals(rec.getObjectKey())) {
                Optional<MediaObjectInfo> videoInfoOpt = mediaStorage.head(req.objectKey());
                if (videoInfoOpt.isEmpty()) {
                    eliminarObjetosSilencioso(req.objectKey(), req.posterKey());
                    throw new UnprocessableEntityException("El video subido no se encuentra en el almacenamiento");
                }
                MediaObjectInfo vi = videoInfoOpt.get();
                if (vi.contentType() == null || !vi.contentType().toLowerCase().contains("mp4")) {
                    eliminarObjetosSilencioso(req.objectKey(), req.posterKey());
                    throw new UnprocessableEntityException("El video debe ser video/mp4");
                }
                if (vi.sizeBytes() > MediaService.MAX_RECOMMENDATION_VIDEO_BYTES) {
                    eliminarObjetosSilencioso(req.objectKey(), req.posterKey());
                    throw new UnprocessableEntityException("El video excede el límite de 15 MB");
                }
                sizeBytes = vi.sizeBytes();
                // Eliminar anterior si existía
                if (rec.getObjectKey() != null) {
                    mediaStorage.delete(rec.getObjectKey());
                }
                rec.setObjectKey(req.objectKey());
            }

            // Si cambió la portada, validar la nueva
            if (!req.posterKey().equals(rec.getPosterKey())) {
                Optional<MediaObjectInfo> posterInfoOpt = mediaStorage.head(req.posterKey());
                if (posterInfoOpt.isEmpty()) {
                    eliminarObjetosSilencioso(req.objectKey(), req.posterKey());
                    throw new UnprocessableEntityException("La portada subida no se encuentra en el almacenamiento");
                }
                MediaObjectInfo pi = posterInfoOpt.get();
                String ct = pi.contentType() != null ? pi.contentType().toLowerCase() : "";
                if (!ct.contains("webp") && !ct.contains("jpeg") && !ct.contains("jpg")) {
                    eliminarObjetosSilencioso(req.objectKey(), req.posterKey());
                    throw new UnprocessableEntityException("La portada debe ser image/webp o image/jpeg");
                }
                if (pi.sizeBytes() > MediaService.MAX_POSTER_BYTES) {
                    eliminarObjetosSilencioso(req.objectKey(), req.posterKey());
                    throw new UnprocessableEntityException("La portada excede el límite de 150 KB");
                }
                // Eliminar anterior si existía
                if (rec.getPosterKey() != null) {
                    mediaStorage.delete(rec.getPosterKey());
                }
                rec.setPosterKey(req.posterKey());
            }

            rec.setTipo(TipoRecomendacion.CLIP);
            rec.setDuracionSeg(duracion);
            rec.setTamanoBytes(sizeBytes);
            rec.setYoutubeId(null);

        } else if (req.tipo() == TipoRecomendacion.YOUTUBE) {
            String ytInput = req.youtubeUrlOrId() != null && !req.youtubeUrlOrId().isBlank()
                    ? req.youtubeUrlOrId()
                    : rec.getYoutubeId();

            String youtubeId = YouTubeUrlExtractor.extractId(ytInput)
                    .orElseThrow(() -> new UnprocessableEntityException("Enlace o ID de YouTube no válido"));

            // Si antes era CLIP, limpiar objetos del storage
            if (rec.getTipo() == TipoRecomendacion.CLIP) {
                if (rec.getObjectKey() != null) mediaStorage.delete(rec.getObjectKey());
                if (rec.getPosterKey() != null) mediaStorage.delete(rec.getPosterKey());
                rec.setObjectKey(null);
                rec.setPosterKey(null);
                rec.setTamanoBytes(null);
            }

            int duracion = req.duracionSeg() != null ? Math.min(req.duracionSeg(), MAX_RECOMMENDATION_DURATION_SEC) : rec.getDuracionSeg();
            rec.setTipo(TipoRecomendacion.YOUTUBE);
            rec.setYoutubeId(youtubeId);
            rec.setDuracionSeg(duracion);
        }

        return toAdminDto(recomendacionRepository.save(rec));
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public void eliminar(Long id) {
        recomendacionRepository.findById(id).ifPresent(rec -> {
            if (rec.getTipo() == TipoRecomendacion.CLIP) {
                if (rec.getObjectKey() != null) mediaStorage.delete(rec.getObjectKey());
                if (rec.getPosterKey() != null) mediaStorage.delete(rec.getPosterKey());
            }
            recomendacionRepository.delete(rec);
            log.info("Recomendación ID {} eliminada", id);
        });
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public AdminRecomendacionDto activar(Long id, boolean activa) {
        Recomendacion rec = recomendacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recomendación no encontrada con ID " + id));
        rec.setActiva(activa);
        return toAdminDto(recomendacionRepository.save(rec));
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public void reordenar(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            int orden = i + 1;
            recomendacionRepository.findById(id).ifPresent(rec -> {
                rec.setOrden(orden);
                recomendacionRepository.save(rec);
            });
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "catalogo", key = "'public_recs_' + #categoriaSlug + '_' + #productoId + '_' + #page + '_' + #size")
    public PublicRecomendacionesResponse obtenerPublicas(String categoriaSlug, Long productoId, int page, int size) {
        int effectivePage = Math.max(0, page);
        int effectiveSize = (size <= 0 || size > 32) ? 8 : size;

        String catFilter = (categoriaSlug != null && !categoriaSlug.isBlank()) ? categoriaSlug.trim() : null;

        Pageable pageable = PageRequest.of(effectivePage, effectiveSize);
        Page<Recomendacion> result = recomendacionRepository.findPublicas(catFilter, productoId, pageable);

        List<PublicRecomendacionDto> items = result.getContent().stream()
                .map(this::toPublicDto)
                .toList();

        return new PublicRecomendacionesResponse(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    private int calcularSiguienteOrden() {
        return recomendacionRepository.findTopByOrderByOrdenDesc()
                .map(r -> r.getOrden() + 1)
                .orElse(1);
    }

    private String limpiarTexto(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private void eliminarObjetosSilencioso(String videoKey, String posterKey) {
        try {
            if (videoKey != null) mediaStorage.delete(videoKey);
        } catch (Exception e) {
            log.warn("Error eliminando objeto de video {}", videoKey, e);
        }
        try {
            if (posterKey != null) mediaStorage.delete(posterKey);
        } catch (Exception e) {
            log.warn("Error eliminando objeto de portada {}", posterKey, e);
        }
    }

    public AdminRecomendacionDto toAdminDto(Recomendacion r) {
        String videoUrl = (r.getObjectKey() != null) ? mediaStorage.publicUrl(r.getObjectKey()) : null;
        String posterUrl = (r.getPosterKey() != null) ? mediaStorage.publicUrl(r.getPosterKey()) : null;
        return new AdminRecomendacionDto(
                r.getId(),
                r.getTitulo(),
                r.getDescripcion(),
                r.getTipo(),
                r.getObjectKey(),
                r.getPosterKey(),
                videoUrl,
                posterUrl,
                r.getYoutubeId(),
                r.getDuracionSeg(),
                r.getTamanoBytes(),
                r.getProducto() != null ? r.getProducto().getId() : null,
                r.getProducto() != null ? r.getProducto().getNombre() : null,
                r.getCategoriaSlug(),
                r.getOrden(),
                r.isActiva(),
                r.getCreadoEn()
        );
    }

    public PublicRecomendacionDto toPublicDto(Recomendacion r) {
        String videoUrl = (r.getObjectKey() != null) ? mediaStorage.publicUrl(r.getObjectKey()) : null;
        String posterUrl = (r.getPosterKey() != null) ? mediaStorage.publicUrl(r.getPosterKey()) : null;
        return new PublicRecomendacionDto(
                r.getId(),
                r.getTitulo(),
                r.getDescripcion(),
                r.getTipo(),
                videoUrl,
                posterUrl,
                r.getYoutubeId(),
                r.getDuracionSeg(),
                r.getProducto() != null ? r.getProducto().getId() : null,
                r.getProducto() != null ? r.getProducto().getNombre() : null,
                r.getCategoriaSlug()
        );
    }
}
