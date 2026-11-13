package com.sheis.catalogo.service.media;

public record BucketHealth(
        boolean habilitado,
        boolean bucketAlcanzable,
        String mensaje
) {}
