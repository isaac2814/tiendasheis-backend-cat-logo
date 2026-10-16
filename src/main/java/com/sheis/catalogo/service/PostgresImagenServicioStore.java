package com.sheis.catalogo.service;

import com.sheis.catalogo.domain.ServicioImagen;
import com.sheis.catalogo.domain.SkuCategoria;
import com.sheis.catalogo.exception.ResourceNotFoundException;
import com.sheis.catalogo.repository.ServicioImagenRepository;
import com.sheis.catalogo.repository.SkuCategoriaRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PostgresImagenServicioStore implements ImagenServicioStore {

    private final SkuCategoriaRepository skuCategoriaRepository;
    private final ServicioImagenRepository servicioImagenRepository;

    public PostgresImagenServicioStore(SkuCategoriaRepository skuCategoriaRepository,
                                       ServicioImagenRepository servicioImagenRepository) {
        this.skuCategoriaRepository = skuCategoriaRepository;
        this.servicioImagenRepository = servicioImagenRepository;
    }

    @Override
    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public ImagenGuardada guardar(String codigoServicio, String contentType, byte[] bytes) {
        SkuCategoria sku = skuCategoriaRepository.findByCodigo(codigoServicio)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con código: " + codigoServicio));

        ServicioImagen imagen = servicioImagenRepository.findById(sku.getId())
                .orElseGet(() -> {
                    ServicioImagen nueva = new ServicioImagen();
                    nueva.setSkuCategoria(sku);
                    return nueva;
                });

        Instant ahora = Instant.now();
        imagen.setContentType(contentType);
        imagen.setDatos(bytes);
        imagen.setBytesLen(bytes.length);
        imagen.setActualizadoEn(ahora);

        sku.setImagen(imagen);
        servicioImagenRepository.saveAndFlush(imagen);

        String url = "/api/public/servicios/" + codigoServicio + "/imagen?v=" + ahora.toEpochMilli();
        return new ImagenGuardada(url, ahora);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ImagenData> obtener(String codigoServicio) {
        return servicioImagenRepository.findByCodigoServicio(codigoServicio)
                .map(img -> new ImagenData(img.getContentType(), img.getDatos(), img.getActualizadoEn()));
    }

    @Override
    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public void eliminar(String codigoServicio) {
        skuCategoriaRepository.findByCodigo(codigoServicio).ifPresent(sku -> {
            if (sku.getImagen() != null) {
                ServicioImagen img = sku.getImagen();
                sku.setImagen(null);
                servicioImagenRepository.delete(img);
            } else {
                servicioImagenRepository.findByCodigoServicio(codigoServicio)
                        .ifPresent(servicioImagenRepository::delete);
            }
        });
        servicioImagenRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Instant> obtenerVersionesPorCodigo() {
        List<Object[]> rows = servicioImagenRepository.findMetadatosVersiones();
        Map<String, Instant> versiones = new HashMap<>(rows.size());
        for (Object[] r : rows) {
            String codigo = (String) r[0];
            Instant actualizadoEn = (Instant) r[1];
            versiones.put(codigo, actualizadoEn);
        }
        return versiones;
    }
}
