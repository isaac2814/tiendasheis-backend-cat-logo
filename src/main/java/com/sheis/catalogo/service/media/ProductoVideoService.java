package com.sheis.catalogo.service.media;

import com.sheis.catalogo.domain.Producto;
import com.sheis.catalogo.domain.ProductoVideo;
import com.sheis.catalogo.exception.ResourceNotFoundException;
import com.sheis.catalogo.exception.ServiceUnavailableException;
import com.sheis.catalogo.exception.UnprocessableEntityException;
import com.sheis.catalogo.repository.ProductoRepository;
import com.sheis.catalogo.repository.ProductoVideoRepository;
import com.sheis.catalogo.web.dto.admin.GuardarVideoProductoRequest;
import com.sheis.catalogo.web.dto.admin.ProductoVideoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
public class ProductoVideoService {

    public static final int MAX_PRODUCT_VIDEO_DURATION_SEC = 20;

    private final ProductoRepository productoRepository;
    private final ProductoVideoRepository productoVideoRepository;
    private final MediaStorage mediaStorage;

    public ProductoVideoService(
            ProductoRepository productoRepository,
            ProductoVideoRepository productoVideoRepository,
            MediaStorage mediaStorage
    ) {
        this.productoRepository = productoRepository;
        this.productoVideoRepository = productoVideoRepository;
        this.mediaStorage = mediaStorage;
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public ProductoVideoDto guardarVideo(Long productoId, GuardarVideoProductoRequest req) {
        if (!mediaStorage.isEnabled()) {
            throw new ServiceUnavailableException("El almacenamiento multimedia en la nube (R2) no está habilitado");
        }

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID " + productoId));

        int duracion = req.duracionSeg();
        if (duracion <= 0 || duracion > MAX_PRODUCT_VIDEO_DURATION_SEC) {
            eliminarObjetosSilencioso(req.objectKey(), req.posterKey());
            throw new UnprocessableEntityException("La duración del video (" + duracion + "s) excede el máximo permitido de " + MAX_PRODUCT_VIDEO_DURATION_SEC + " segundos");
        }

        // Validar existencia y metadatos del video en el storage
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
        if (videoInfo.sizeBytes() > MediaService.MAX_PRODUCT_VIDEO_BYTES) {
            eliminarObjetosSilencioso(req.objectKey(), req.posterKey());
            throw new UnprocessableEntityException("El archivo de video excede el límite de 8 MB");
        }

        // Validar existencia y metadatos de la portada en el storage
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

        // Manejar reemplazo si ya existía un video
        Optional<ProductoVideo> existenteOpt = productoVideoRepository.findByProductoId(productoId);
        ProductoVideo video;
        if (existenteOpt.isPresent()) {
            video = existenteOpt.get();
            if (!video.getObjectKey().equals(req.objectKey())) {
                mediaStorage.delete(video.getObjectKey());
            }
            if (!video.getPosterKey().equals(req.posterKey())) {
                mediaStorage.delete(video.getPosterKey());
            }
            video.setObjectKey(req.objectKey());
            video.setPosterKey(req.posterKey());
            video.setDuracionSeg(duracion);
            video.setTamanoBytes(videoInfo.sizeBytes());
        } else {
            video = new ProductoVideo(
                    producto,
                    req.objectKey(),
                    req.posterKey(),
                    duracion,
                    videoInfo.sizeBytes()
            );
        }

        ProductoVideo guardado = productoVideoRepository.save(video);

        return toDto(guardado);
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public void eliminarVideo(Long productoId) {
        productoVideoRepository.findByProductoId(productoId).ifPresent(video -> {
            mediaStorage.delete(video.getObjectKey());
            mediaStorage.delete(video.getPosterKey());
            productoVideoRepository.delete(video);
            log.info("Video eliminado para producto ID {}", productoId);
        });
    }

    @Transactional(readOnly = true)
    public Optional<ProductoVideoDto> obtenerVideoPorProducto(Long productoId) {
        return productoVideoRepository.findByProductoId(productoId).map(this::toDto);
    }

    private void eliminarObjetosSilencioso(String videoKey, String posterKey) {
        try {
            if (videoKey != null) mediaStorage.delete(videoKey);
        } catch (Exception e) {
            log.warn("Error limpiando video {}", videoKey, e);
        }
        try {
            if (posterKey != null) mediaStorage.delete(posterKey);
        } catch (Exception e) {
            log.warn("Error limpiando poster {}", posterKey, e);
        }
    }

    public ProductoVideoDto toDto(ProductoVideo entity) {
        return new ProductoVideoDto(
                entity.getId(),
                entity.getProducto().getId(),
                entity.getObjectKey(),
                entity.getPosterKey(),
                mediaStorage.publicUrl(entity.getObjectKey()),
                mediaStorage.publicUrl(entity.getPosterKey()),
                entity.getDuracionSeg(),
                entity.getTamanoBytes()
        );
    }
}
