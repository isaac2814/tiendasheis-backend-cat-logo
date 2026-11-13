package com.sheis.catalogo.service.media;

import com.sheis.catalogo.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class NoopMediaStorage implements MediaStorage {

    @Override
    public PresignedUpload presignUpload(String key, String contentType, long maxBytes) {
        throw new ServiceUnavailableException("El almacenamiento multimedia en la nube (R2) no está habilitado");
    }

    @Override
    public Optional<MediaObjectInfo> head(String key) {
        return Optional.empty();
    }

    @Override
    public void delete(String key) {
        log.debug("NoopMediaStorage: delete({}) ignorado porque R2 está deshabilitado", key);
    }

    @Override
    public String publicUrl(String key) {
        return "";
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public BucketHealth checkHealth() {
        return new BucketHealth(false, false, "Almacenamiento multimedia R2 no habilitado");
    }
}
