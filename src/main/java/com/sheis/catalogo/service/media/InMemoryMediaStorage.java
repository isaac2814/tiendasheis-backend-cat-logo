package com.sheis.catalogo.service.media;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryMediaStorage implements MediaStorage {

    private final Map<String, MediaObjectInfo> objects = new ConcurrentHashMap<>();
    private final String publicBaseUrl;
    private boolean reachable = true;

    public InMemoryMediaStorage() {
        this("https://pub-test.r2.dev");
    }

    public InMemoryMediaStorage(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl != null ? publicBaseUrl.replaceAll("/+$", "") : "https://pub-test.r2.dev";
    }

    public void putMockObject(String key, String contentType, long sizeBytes) {
        String cleanKey = key.startsWith("/") ? key.substring(1) : key;
        objects.put(cleanKey, new MediaObjectInfo(sizeBytes, contentType));
    }

    public void setReachable(boolean reachable) {
        this.reachable = reachable;
    }

    public void clear() {
        objects.clear();
    }

    @Override
    public PresignedUpload presignUpload(String key, String contentType, long maxBytes) {
        String cleanKey = key.startsWith("/") ? key.substring(1) : key;
        String uploadUrl = "https://r2-mock.local/upload/" + cleanKey;
        String publicUrl = publicUrl(cleanKey);
        Map<String, String> headers = Map.of(
                "Content-Type", contentType,
                "Cache-Control", "public, max-age=31536000, immutable"
        );
        return new PresignedUpload(uploadUrl, cleanKey, publicUrl, 600, headers);
    }

    @Override
    public Optional<MediaObjectInfo> head(String key) {
        String cleanKey = key.startsWith("/") ? key.substring(1) : key;
        return Optional.ofNullable(objects.get(cleanKey));
    }

    @Override
    public void delete(String key) {
        String cleanKey = key.startsWith("/") ? key.substring(1) : key;
        objects.remove(cleanKey);
    }

    @Override
    public String publicUrl(String key) {
        if (key == null || key.isBlank()) return "";
        String cleanKey = key.startsWith("/") ? key.substring(1) : key;
        return publicBaseUrl + "/" + cleanKey;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public BucketHealth checkHealth() {
        if (reachable) {
            return new BucketHealth(true, true, "Bucket R2 alcanzable y configurado correctamente (Mock)");
        }
        return new BucketHealth(true, false, "Error conectando al bucket R2 (Mock)");
    }
}
