package com.sheis.catalogo.service.media;

import java.util.Map;

public record PresignedUpload(
        String uploadUrl,
        String objectKey,
        String publicUrl,
        int expiresInSeconds,
        Map<String, String> headers
) {}
