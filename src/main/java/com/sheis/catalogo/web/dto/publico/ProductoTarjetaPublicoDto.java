package com.sheis.catalogo.web.dto.publico;

public record ProductoTarjetaPublicoDto(
        Long id,
        String slug,
        String nombre,
        String lema,
        String imagen,
        long precioDesde,
        boolean tieneVariantes,
        String stockEstado,
        boolean precioVaria,
        Long categoriaId,
        String categoriaNombre,
        String categoriaSlug,
        boolean tieneVideo,
        Integer duracionSeg
) {
    public ProductoTarjetaPublicoDto(
            Long id,
            String slug,
            String nombre,
            String lema,
            String imagen,
            long precioDesde,
            boolean tieneVariantes,
            String stockEstado,
            boolean precioVaria,
            Long categoriaId,
            String categoriaNombre,
            String categoriaSlug
    ) {
        this(id, slug, nombre, lema, imagen, precioDesde, tieneVariantes, stockEstado, precioVaria, categoriaId, catNombreOrNull(categoriaNombre), categoriaSlug, false, null);
    }

    private static String catNombreOrNull(String name) {
        return name;
    }
}
