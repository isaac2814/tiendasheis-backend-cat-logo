package com.sheis.catalogo.service.media;

import com.sheis.catalogo.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class R2MediaStorage implements MediaStorage {

    private static final String CACHE_CONTROL_VALUE = "public, max-age=31536000, immutable";
    private static final Duration PRESIGN_DURATION = Duration.ofMinutes(10);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AppProperties.R2 r2Config;

    public R2MediaStorage(S3Client s3Client, S3Presigner s3Presigner, AppProperties.R2 r2Config) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.r2Config = r2Config;
    }

    @Override
    public PresignedUpload presignUpload(String key, String contentType, long maxBytes) {
        String cleanKey = key.startsWith("/") ? key.substring(1) : key;
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(r2Config.getBucket())
                .key(cleanKey)
                .contentType(contentType)
                .cacheControl(CACHE_CONTROL_VALUE)
                .build();

        PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
                .signatureDuration(PRESIGN_DURATION)
                .putObjectRequest(putReq)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignReq);

        Map<String, String> headers = Map.of(
                "Content-Type", contentType,
                "Cache-Control", CACHE_CONTROL_VALUE
        );

        return new PresignedUpload(
                presigned.url().toString(),
                cleanKey,
                publicUrl(cleanKey),
                (int) PRESIGN_DURATION.toSeconds(),
                headers
        );
    }

    @Override
    public Optional<MediaObjectInfo> head(String key) {
        String cleanKey = key.startsWith("/") ? key.substring(1) : key;
        try {
            HeadObjectRequest req = HeadObjectRequest.builder()
                    .bucket(r2Config.getBucket())
                    .key(cleanKey)
                    .build();
            HeadObjectResponse resp = s3Client.headObject(req);
            return Optional.of(new MediaObjectInfo(resp.contentLength(), resp.contentType()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            log.warn("Error consultando objeto en R2 {}: {}", cleanKey, e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Error inesperado en headObject R2 {}: {}", cleanKey, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isBlank()) return;
        String cleanKey = key.startsWith("/") ? key.substring(1) : key;
        try {
            DeleteObjectRequest req = DeleteObjectRequest.builder()
                    .bucket(r2Config.getBucket())
                    .key(cleanKey)
                    .build();
            s3Client.deleteObject(req);
            log.info("Objeto eliminado de R2: {}", cleanKey);
        } catch (Exception e) {
            log.warn("No se pudo eliminar el objeto {} de R2: {}", cleanKey, e.getMessage());
        }
    }

    @Override
    public String publicUrl(String key) {
        if (key == null || key.isBlank()) return "";
        String cleanKey = key.startsWith("/") ? key.substring(1) : key;
        String base = r2Config.getNormalizedPublicBaseUrl();
        if (base.isEmpty()) {
            return "/" + cleanKey;
        }
        return base + "/" + cleanKey;
    }

    @Override
    public boolean isEnabled() {
        return r2Config.isEnabled();
    }

    @Override
    public BucketHealth checkHealth() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(r2Config.getBucket()).build());
            return new BucketHealth(true, true, "Bucket R2 alcanzable y configurado correctamente");
        } catch (Exception e) {
            log.warn("Fallo comprobando estado del bucket R2: {}", e.getMessage());
            return new BucketHealth(true, false, "Error conectando al bucket R2: " + e.getMessage());
        }
    }
}
