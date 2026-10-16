package com.sheis.catalogo.service;

import com.sheis.catalogo.domain.MetodoPago;
import com.sheis.catalogo.domain.MetodoPagoImagen;
import com.sheis.catalogo.exception.ResourceNotFoundException;
import com.sheis.catalogo.repository.MetodoPagoImagenRepository;
import com.sheis.catalogo.repository.MetodoPagoRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PostgresImagenMetodoPagoStore implements ImagenMetodoPagoStore {

    private final MetodoPagoRepository metodoPagoRepository;
    private final MetodoPagoImagenRepository metodoPagoImagenRepository;

    public PostgresImagenMetodoPagoStore(MetodoPagoRepository metodoPagoRepository,
                                         MetodoPagoImagenRepository metodoPagoImagenRepository) {
        this.metodoPagoRepository = metodoPagoRepository;
        this.metodoPagoImagenRepository = metodoPagoImagenRepository;
    }

    @Override
    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public ImagenGuardada guardar(String metodoPagoId, String contentType, byte[] bytes) {
        MetodoPago metodo = metodoPagoRepository.findById(metodoPagoId)
                .orElseThrow(() -> new ResourceNotFoundException("Método de pago no encontrado con id: " + metodoPagoId));

        MetodoPagoImagen imagen = metodoPagoImagenRepository.findById(metodoPagoId)
                .orElseGet(() -> {
                    MetodoPagoImagen nueva = new MetodoPagoImagen();
                    nueva.setMetodoPago(metodo);
                    return nueva;
                });

        Instant ahora = Instant.now();
        imagen.setContentType(contentType);
        imagen.setDatos(bytes);
        imagen.setBytesLen(bytes.length);
        imagen.setActualizadoEn(ahora);

        metodo.setImagen(imagen);
        metodoPagoImagenRepository.saveAndFlush(imagen);

        String url = "/api/public/metodos-pago/" + metodoPagoId + "/imagen?v=" + ahora.toEpochMilli();
        return new ImagenGuardada(url, ahora);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ImagenData> obtener(String metodoPagoId) {
        return metodoPagoImagenRepository.findByMetodoPagoId(metodoPagoId)
                .map(img -> new ImagenData(img.getContentType(), img.getDatos(), img.getActualizadoEn()));
    }

    @Override
    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public void eliminar(String metodoPagoId) {
        metodoPagoRepository.findById(metodoPagoId).ifPresent(metodo -> {
            if (metodo.getImagen() != null) {
                MetodoPagoImagen img = metodo.getImagen();
                metodo.setImagen(null);
                metodoPagoImagenRepository.delete(img);
            } else {
                metodoPagoImagenRepository.findByMetodoPagoId(metodoPagoId)
                        .ifPresent(metodoPagoImagenRepository::delete);
            }
        });
        metodoPagoImagenRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Instant> obtenerVersionesPorId() {
        List<Object[]> rows = metodoPagoImagenRepository.findMetadatosVersiones();
        Map<String, Instant> versiones = new HashMap<>(rows.size());
        for (Object[] r : rows) {
            String id = (String) r[0];
            Instant actualizadoEn = (Instant) r[1];
            versiones.put(id, actualizadoEn);
        }
        return versiones;
    }
}
