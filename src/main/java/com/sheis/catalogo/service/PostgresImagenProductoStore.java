package com.sheis.catalogo.service;

import com.sheis.catalogo.domain.Producto;
import com.sheis.catalogo.domain.ProductoImagen;
import com.sheis.catalogo.exception.BadRequestException;
import com.sheis.catalogo.exception.ResourceNotFoundException;
import com.sheis.catalogo.repository.ProductoImagenRepository;
import com.sheis.catalogo.repository.ProductoRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class PostgresImagenProductoStore implements ImagenProductoStore {

    public static final int MAX_IMAGENES_POR_PRODUCTO = 6;

    private final ProductoRepository productoRepository;
    private final ProductoImagenRepository productoImagenRepository;

    public PostgresImagenProductoStore(ProductoRepository productoRepository,
                                       ProductoImagenRepository productoImagenRepository) {
        this.productoRepository = productoRepository;
        this.productoImagenRepository = productoImagenRepository;
    }

    @Override
    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public ImagenGuardada guardar(Long productoId, String contentType, byte[] bytes, Integer ordenDeseado) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + productoId));

        int totalActual = productoImagenRepository.countByProductoId(productoId);
        if (totalActual >= MAX_IMAGENES_POR_PRODUCTO) {
            throw new BadRequestException("El producto ya tiene el máximo permitido de " + MAX_IMAGENES_POR_PRODUCTO + " imágenes");
        }

        String detectedType = ImageValidator.validarYDetectarContentType(bytes, contentType);

        int orden = ordenDeseado != null ? ordenDeseado :
                productoImagenRepository.findMaxOrdenByProductoId(productoId).orElse(0) + 1;

        Instant ahora = Instant.now();
        ProductoImagen imagen = new ProductoImagen(producto, orden, detectedType, bytes, bytes.length);
        imagen.setCreadoEn(ahora);
        imagen.setActualizadoEn(ahora);

        producto.getImagenes().add(imagen);
        ProductoImagen guardada = productoImagenRepository.saveAndFlush(imagen);

        String url = "/api/public/productos/" + productoId + "/imagenes/" + guardada.getId() + "?v=" + ahora.toEpochMilli();
        return new ImagenGuardada(guardada.getId(), url, guardada.getOrden(), ahora);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ImagenData> obtener(Long productoId, Long imagenId) {
        return productoImagenRepository.findByIdAndProductoId(imagenId, productoId)
                .map(pi -> new ImagenData(pi.getTipo(), pi.getContenido(), pi.getActualizadoEn()));
    }

    @Override
    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public void eliminar(Long productoId, Long imagenId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + productoId));

        ProductoImagen imagen = productoImagenRepository.findByIdAndProductoId(imagenId, productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Imagen no encontrada con ID: " + imagenId));

        producto.getImagenes().remove(imagen);
        productoImagenRepository.delete(imagen);
        productoImagenRepository.flush();

        // Reordenar las imágenes restantes 1..N
        List<ProductoImagen> restantes = productoImagenRepository.findByProductoIdOrderByOrdenAscIdAsc(productoId);
        for (int i = 0; i < restantes.size(); i++) {
            restantes.get(i).setOrden(i + 1);
            restantes.get(i).setActualizadoEn(Instant.now());
        }
        if (!restantes.isEmpty()) {
            productoImagenRepository.saveAllAndFlush(restantes);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public List<ImagenGuardada> reordenar(Long productoId, List<Long> imagenesIdsOrdenados) {
        if (!productoRepository.existsById(productoId)) {
            throw new ResourceNotFoundException("Producto no encontrado con ID: " + productoId);
        }

        List<ProductoImagen> actuales = productoImagenRepository.findByProductoIdOrderByOrdenAscIdAsc(productoId);
        Map<Long, ProductoImagen> mapById = new HashMap<>();
        for (ProductoImagen pi : actuales) {
            mapById.put(pi.getId(), pi);
        }

        if (imagenesIdsOrdenados == null || imagenesIdsOrdenados.isEmpty()) {
            return listar(productoId);
        }

        int orden = 1;
        Instant ahora = Instant.now();
        List<ProductoImagen> reordenadas = new ArrayList<>();
        Set<Long> procesados = new HashSet<>();

        for (Long id : imagenesIdsOrdenados) {
            ProductoImagen pi = mapById.get(id);
            if (pi != null && procesados.add(id)) {
                pi.setOrden(orden++);
                pi.setActualizadoEn(ahora);
                reordenadas.add(pi);
            }
        }

        // Agregar las que no venían en la lista manteniendo su orden relativo
        for (ProductoImagen pi : actuales) {
            if (!procesados.contains(pi.getId())) {
                pi.setOrden(orden++);
                pi.setActualizadoEn(ahora);
                reordenadas.add(pi);
            }
        }

        productoImagenRepository.saveAllAndFlush(reordenadas);
        return listar(productoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImagenGuardada> listar(Long productoId) {
        List<ProductoImagen> imagenes = productoImagenRepository.findByProductoIdOrderByOrdenAscIdAsc(productoId);
        return imagenes.stream()
                .map(pi -> new ImagenGuardada(
                        pi.getId(),
                        "/api/public/productos/" + productoId + "/imagenes/" + pi.getId() + "?v=" + pi.getActualizadoEn().toEpochMilli(),
                        pi.getOrden(),
                        pi.getActualizadoEn()
                ))
                .toList();
    }
}
