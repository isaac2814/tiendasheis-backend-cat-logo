package com.sheis.catalogo.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public interface ImagenServicioStore {

    ImagenGuardada guardar(String codigoServicio, String contentType, byte[] bytes);

    Optional<ImagenData> obtener(String codigoServicio);

    void eliminar(String codigoServicio);

    Map<String, Instant> obtenerVersionesPorCodigo();

    record ImagenData(String contentType, byte[] datos, Instant actualizadoEn) {}

    record ImagenGuardada(String imagenUrl, Instant actualizadoEn) {}
}
