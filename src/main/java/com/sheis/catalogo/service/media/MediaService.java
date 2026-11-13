package com.sheis.catalogo.service.media;

import com.sheis.catalogo.config.AppProperties;
import com.sheis.catalogo.exception.ServiceUnavailableException;
import com.sheis.catalogo.exception.UnprocessableEntityException;
import com.sheis.catalogo.web.dto.admin.MediaEstadoResponse;
import com.sheis.catalogo.web.dto.admin.MediaKind;
import com.sheis.catalogo.web.dto.admin.PresignRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class MediaService {

    public static final long MAX_PRODUCT_VIDEO_BYTES = 8L * 1024 * 1024; // 8 MB
    public static final long MAX_RECOMMENDATION_VIDEO_BYTES = 15L * 1024 * 1024; // 15 MB
    public static final long MAX_POSTER_BYTES = 150L * 1024; // 150 KB

    private final MediaStorage mediaStorage;
    private final AppProperties.R2 r2Config;

    public MediaService(MediaStorage mediaStorage, AppProperties appProperties) {
        this.mediaStorage = mediaStorage;
        this.r2Config = appProperties.getR2();
    }

    public MediaEstadoResponse getEstado() {
        boolean enabled = mediaStorage.isEnabled();
        BucketHealth health = mediaStorage.checkHealth();
        return new MediaEstadoResponse(
                enabled,
                r2Config != null ? r2Config.getBucket() : "",
                r2Config != null ? r2Config.getPublicBaseUrl() : "",
                health.bucketAlcanzable(),
                health.mensaje()
        );
    }

    public PresignedUpload presign(PresignRequest req) {
        if (!mediaStorage.isEnabled()) {
            throw new ServiceUnavailableException("El almacenamiento multimedia en la nube (R2) no está habilitado");
        }

        MediaKind kind = req.kind();
        String contentType = req.contentType() != null ? req.contentType().trim().toLowerCase() : "";
        long sizeBytes = req.sizeBytes() != null ? req.sizeBytes() : 0L;

        String key;
        long maxBytes;

        switch (kind) {
            case PRODUCT_VIDEO -> {
                if (!"video/mp4".equalsIgnoreCase(contentType)) {
                    throw new UnprocessableEntityException("Formato no permitido para video de producto: se requiere video/mp4");
                }
                maxBytes = MAX_PRODUCT_VIDEO_BYTES;
                if (sizeBytes > maxBytes) {
                    throw new UnprocessableEntityException("El video del producto excede el límite de 8 MB (tamaño: " + (sizeBytes / (1024 * 1024)) + " MB)");
                }
                key = "videos/" + UUID.randomUUID() + ".mp4";
            }
            case RECOMMENDATION_VIDEO -> {
                if (!"video/mp4".equalsIgnoreCase(contentType)) {
                    throw new UnprocessableEntityException("Formato no permitido para recomendación: se requiere video/mp4");
                }
                maxBytes = MAX_RECOMMENDATION_VIDEO_BYTES;
                if (sizeBytes > maxBytes) {
                    throw new UnprocessableEntityException("El video de recomendación excede el límite de 15 MB (tamaño: " + (sizeBytes / (1024 * 1024)) + " MB)");
                }
                key = "videos/" + UUID.randomUUID() + ".mp4";
            }
            case POSTER -> {
                if (!"image/webp".equalsIgnoreCase(contentType) && !"image/jpeg".equalsIgnoreCase(contentType)) {
                    throw new UnprocessableEntityException("Formato no permitido para portada: se requiere image/webp o image/jpeg");
                }
                maxBytes = MAX_POSTER_BYTES;
                if (sizeBytes > maxBytes) {
                    throw new UnprocessableEntityException("La portada excede el límite de 150 KB (tamaño: " + (sizeBytes / 1024) + " KB)");
                }
                String ext = "image/jpeg".equalsIgnoreCase(contentType) ? ".jpeg" : ".webp";
                key = "posters/" + UUID.randomUUID() + ext;
            }
            default -> throw new UnprocessableEntityException("Tipo de medio no soportado: " + kind);
        }

        return mediaStorage.presignUpload(key, contentType, sizeBytes > 0 ? sizeBytes : maxBytes);
    }

    public MediaStorage getStorage() {
        return mediaStorage;
    }
}
