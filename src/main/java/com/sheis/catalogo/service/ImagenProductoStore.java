package com.sheis.catalogo.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ImagenProductoStore {

    ImagenGuardada guardar(Long productoId, String contentType, byte[] bytes, Integer orden);

    Optional<ImagenData> obtener(Long productoId, Long imagenId);

    void eliminar(Long productoId, Long imagenId);

    List<ImagenGuardada> reordenar(Long productoId, List<Long> imagenesIdsOrdenados);

    List<ImagenGuardada> listar(Long productoId);

    record ImagenData(String contentType, byte[] datos, Instant actualizadoEn) {}

    record ImagenGuardada(Long id, String imagenUrl, int orden, Instant actualizadoEn) {}
}
