package com.sheis.catalogo.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public interface ImagenMetodoPagoStore {

    ImagenGuardada guardar(String metodoPagoId, String contentType, byte[] bytes);

    Optional<ImagenData> obtener(String metodoPagoId);

    void eliminar(String metodoPagoId);

    Map<String, Instant> obtenerVersionesPorId();

    record ImagenData(String contentType, byte[] datos, Instant actualizadoEn) {}

    record ImagenGuardada(String imagenUrl, Instant actualizadoEn) {}
}
