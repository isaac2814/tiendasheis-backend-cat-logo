package com.sheis.catalogo.service.media;

import java.util.Optional;

public interface MediaStorage {

    PresignedUpload presignUpload(String key, String contentType, long maxBytes);

    Optional<MediaObjectInfo> head(String key);

    void delete(String key);

    String publicUrl(String key);

    boolean isEnabled();

    BucketHealth checkHealth();
}
