package com.sheis.catalogo.service;

import com.sheis.catalogo.exception.BadRequestException;

public final class ImageValidator {

    public static final int MAX_BYTES = 512 * 1024; // 512 KB

    private ImageValidator() {
    }

    public static String validarYDetectarContentType(byte[] bytes, String declaredContentType) {
        if (bytes == null || bytes.length == 0) {
            throw new BadRequestException("El archivo de imagen está vacío");
        }

        if (bytes.length > MAX_BYTES) {
            throw new BadRequestException("La imagen excede el tamaño máximo permitido de 512 KB");
        }

        String detected = detectContentType(bytes);
        if (detected == null) {
            throw new BadRequestException("El archivo no es una imagen válida (debe ser PNG, JPG o WebP)");
        }

        return detected;
    }

    public static String detectContentType(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return null;
        }

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if ((bytes[0] & 0xFF) == 0x89 &&
                bytes[1] == 'P' &&
                bytes[2] == 'N' &&
                bytes[3] == 'G' &&
                bytes[4] == 0x0D &&
                bytes[5] == 0x0A &&
                bytes[6] == 0x1A &&
                bytes[7] == 0x0A) {
            return "image/png";
        }

        // JPEG: FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF &&
                (bytes[1] & 0xFF) == 0xD8 &&
                (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }

        // WebP: RIFF (bytes 0..3) ... WEBP (bytes 8..11)
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' &&
                bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }

        return null;
    }
}
