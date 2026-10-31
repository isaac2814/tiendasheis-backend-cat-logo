package com.sheis.catalogo.util;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugUtils {

    private SlugUtils() {
    }

    /**
     * Normaliza un texto para comparaciones de búsqueda:
     * pasa a minúsculas, remueve tildes y diacríticos y elimina espacios en los extremos.
     */
    public static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    /**
     * Convierte un texto en un slug URL-friendly limpio:
     * p. ej. "AirPods Pro 2da Generación" -> "airpods-pro-2da-generacion".
     */
    public static String toSlug(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        String norm = normalize(s);
        String slug = norm.replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "item" : slug;
    }
}
