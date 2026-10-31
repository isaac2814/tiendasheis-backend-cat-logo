package com.sheis.catalogo.service;

import com.sheis.catalogo.domain.CategoriaProducto;
import com.sheis.catalogo.domain.Producto;
import com.sheis.catalogo.domain.ProductoImagen;
import com.sheis.catalogo.domain.ProductoVariante;
import com.sheis.catalogo.exception.ResourceNotFoundException;
import com.sheis.catalogo.repository.CategoriaProductoRepository;
import com.sheis.catalogo.repository.ProductoImagenRepository;
import com.sheis.catalogo.repository.ProductoRepository;
import com.sheis.catalogo.repository.ProductoVarianteRepository;
import com.sheis.catalogo.util.SlugUtils;
import com.sheis.catalogo.web.dto.publico.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductoPublicoService {

    private final ProductoRepository productoRepository;
    private final ProductoVarianteRepository productoVarianteRepository;
    private final ProductoImagenRepository productoImagenRepository;
    private final CategoriaProductoRepository categoriaProductoRepository;

    public ProductoPublicoService(ProductoRepository productoRepository,
                                  ProductoVarianteRepository productoVarianteRepository,
                                  ProductoImagenRepository productoImagenRepository,
                                  CategoriaProductoRepository categoriaProductoRepository) {
        this.productoRepository = productoRepository;
        this.productoVarianteRepository = productoVarianteRepository;
        this.productoImagenRepository = productoImagenRepository;
        this.categoriaProductoRepository = categoriaProductoRepository;
    }

    /**
     * Consulta paginada, filtrada y ordenada de productos tecnológicos activos.
     */
    @Transactional(readOnly = true)
    public ProductosPaginadosResponse obtenerProductos(String categoria,
                                                      String q,
                                                      String sort,
                                                      Integer page,
                                                      Integer size) {
        int pageNum = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size >= 1) ? Math.min(size, 48) : 12;
        String sortType = (sort != null && !sort.isBlank()) ? sort.trim().toLowerCase(Locale.ROOT) : "relevantes";

        String qTrim = (q != null) ? q.trim() : "";
        if (qTrim.length() > 60) {
            qTrim = qTrim.substring(0, 60);
        }
        String qNorm = SlugUtils.normalize(qTrim);

        String catTrim = (categoria != null) ? categoria.trim() : "";
        boolean filtrarCat = !catTrim.isEmpty() &&
                !catTrim.equalsIgnoreCase("todos") &&
                !catTrim.equalsIgnoreCase("all");
        Long catIdParsed = parseLongOrNull(catTrim);
        String catSlugTarget = SlugUtils.toSlug(catTrim);

        // 1. Cargar productos visibles y sus relaciones activas
        List<Producto> visibles = productoRepository.findByVisibleTrueOrderByOrdenAscIdAsc();

        // Variantes activas agrupadas por productoId
        Map<Long, List<ProductoVariante>> variantesPorProducto = productoVarianteRepository
                .findByActivaTrueOrderByOrdenAscIdAsc().stream()
                .collect(Collectors.groupingBy(v -> v.getProducto().getId()));

        // Primera imagen por productoId
        Map<Long, ProductoImagenMeta> primeraImagenPorProducto = productoImagenRepository
                .findAllMetasOrderByOrdenAscIdAsc().stream()
                .collect(Collectors.toMap(
                        ProductoImagenMeta::productoId,
                        img -> img,
                        (existing, replacement) -> existing
                ));

        // 2. Mapear y filtrar productos válidos (con al menos una variante activa)
        List<ProductoTarjetaPublicoDto> filtrados = new ArrayList<>();

        for (Producto p : visibles) {
            List<ProductoVariante> vars = variantesPorProducto.getOrDefault(p.getId(), List.of());
            if (vars.isEmpty()) {
                continue; // Regla 5: Solo productos con al menos una variante activa
            }

            // Filtro por categoría (por ID o por slug)
            if (filtrarCat) {
                Long pCatId = p.getCategoria().getId();
                String pCatSlug = SlugUtils.toSlug(p.getCategoria().getNombre());
                boolean coincideId = catIdParsed != null && pCatId.equals(catIdParsed);
                boolean coincideSlug = pCatSlug.equalsIgnoreCase(catSlugTarget) ||
                        SlugUtils.normalize(p.getCategoria().getNombre()).equals(SlugUtils.normalize(catTrim));
                if (!coincideId && !coincideSlug) {
                    continue;
                }
            }

            // Filtro de búsqueda q (sin tildes, case-insensitive sobre nombre, lema y categoría)
            if (!qNorm.isEmpty()) {
                String nombreNorm = SlugUtils.normalize(p.getNombre());
                String lemaNorm = SlugUtils.normalize(p.getLema());
                String catNorm = SlugUtils.normalize(p.getCategoria().getNombre());
                if (!nombreNorm.contains(qNorm) && !lemaNorm.contains(qNorm) && !catNorm.contains(qNorm)) {
                    continue;
                }
            }

            // Calcular precio desde, precio varía y stock total
            long minPrecio = Long.MAX_VALUE;
            long maxPrecio = Long.MIN_VALUE;
            int stockTotal = 0;

            for (ProductoVariante v : vars) {
                long pr = v.getPrecio().longValue();
                if (pr < minPrecio) minPrecio = pr;
                if (pr > maxPrecio) maxPrecio = pr;
                stockTotal += (v.getStock() != null ? v.getStock() : 0);
            }

            String stockEstado = stockTotal <= 0 ? "AGOTADO" : (stockTotal <= 5 ? "POCAS" : "DISPONIBLE");
            boolean tieneVariantes = vars.size() > 1;
            boolean precioVaria = minPrecio != maxPrecio;

            ProductoImagenMeta primeraImg = primeraImagenPorProducto.get(p.getId());
            String imagenUrl = (primeraImg != null)
                    ? "/api/public/productos/" + p.getId() + "/imagenes/" + primeraImg.id() + "?v=" + primeraImg.actualizadoEn().toEpochMilli()
                    : null;

            String catNombre = p.getCategoria().getNombre();
            String catSlug = SlugUtils.toSlug(catNombre);
            String prodSlug = SlugUtils.toSlug(p.getNombre());

            filtrados.add(new ProductoTarjetaPublicoDto(
                    p.getId(),
                    prodSlug,
                    p.getNombre(),
                    p.getLema(),
                    imagenUrl,
                    minPrecio == Long.MAX_VALUE ? 0 : minPrecio,
                    tieneVariantes,
                    stockEstado,
                    precioVaria,
                    p.getCategoria().getId(),
                    catNombre,
                    catSlug
            ));
        }

        // 3. Ordenación
        Comparator<ProductoTarjetaPublicoDto> comparator = switch (sortType) {
            case "precio_asc" -> Comparator.comparingLong(ProductoTarjetaPublicoDto::precioDesde)
                    .thenComparing(ProductoTarjetaPublicoDto::id);
            case "precio_desc" -> Comparator.comparingLong(ProductoTarjetaPublicoDto::precioDesde).reversed()
                    .thenComparing(ProductoTarjetaPublicoDto::id);
            case "nuevos" -> Comparator.comparing(ProductoTarjetaPublicoDto::id).reversed();
            default -> { // "relevantes": disponibles primero, agotados al final; mantiene orden de inserción/configurado
                yield Comparator.<ProductoTarjetaPublicoDto, Integer>comparing(dto -> "AGOTADO".equals(dto.stockEstado()) ? 1 : 0);
            }
        };

        filtrados.sort(comparator);

        // 4. Paginación
        int totalElements = filtrados.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / pageSize);

        int fromIndex = pageNum * pageSize;
        List<ProductoTarjetaPublicoDto> content;

        if (fromIndex >= totalElements) {
            content = List.of();
        } else {
            int toIndex = Math.min(fromIndex + pageSize, totalElements);
            content = filtrados.subList(fromIndex, toIndex);
        }

        boolean hasNext = (pageNum + 1) * pageSize < totalElements;

        return new ProductosPaginadosResponse(
                content,
                pageNum,
                pageSize,
                totalElements,
                totalPages,
                hasNext
        );
    }

    /**
     * Categorías activas con conteo de productos disponibles y total general.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "catalogo", key = "'public_productos_categorias'")
    public List<CategoriaConteoDto> obtenerCategoriasConConteo() {
        List<CategoriaProducto> categorias = categoriaProductoRepository.findByVisibleTrueOrderByOrdenAscIdAsc();
        List<Producto> productosVisibles = productoRepository.findByVisibleTrueOrderByOrdenAscIdAsc();

        Map<Long, List<ProductoVariante>> variantesPorProducto = productoVarianteRepository
                .findByActivaTrueOrderByOrdenAscIdAsc().stream()
                .collect(Collectors.groupingBy(v -> v.getProducto().getId()));

        Map<Long, Long> conteoPorCategoria = new HashMap<>();
        long totalGeneral = 0;

        for (Producto p : productosVisibles) {
            List<ProductoVariante> vars = variantesPorProducto.getOrDefault(p.getId(), List.of());
            if (!vars.isEmpty()) {
                Long catId = p.getCategoria().getId();
                conteoPorCategoria.put(catId, conteoPorCategoria.getOrDefault(catId, 0L) + 1L);
                totalGeneral++;
            }
        }

        List<CategoriaConteoDto> resultado = new ArrayList<>();
        // Total general ("Todos") como primer elemento
        resultado.add(new CategoriaConteoDto(null, "Todos", "todos", totalGeneral));

        for (CategoriaProducto c : categorias) {
            long totalCat = conteoPorCategoria.getOrDefault(c.getId(), 0L);
            resultado.add(new CategoriaConteoDto(
                    c.getId(),
                    c.getNombre(),
                    SlugUtils.toSlug(c.getNombre()),
                    totalCat
            ));
        }

        return resultado;
    }

    /**
     * Detalle completo de un producto por ID o por Slug.
     */
    @Transactional(readOnly = true)
    public ProductoPublicoDto obtenerDetalleProducto(String idOrSlug) {
        Long id = parseLongOrNull(idOrSlug);
        Producto producto;

        if (id != null) {
            producto = productoRepository.findById(id)
                    .filter(p -> Boolean.TRUE.equals(p.getVisible()))
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + idOrSlug));
        } else {
            String slugTarget = SlugUtils.toSlug(idOrSlug);
            producto = productoRepository.findByVisibleTrueOrderByOrdenAscIdAsc().stream()
                    .filter(p -> SlugUtils.toSlug(p.getNombre()).equalsIgnoreCase(slugTarget))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + idOrSlug));
        }

        List<ProductoVariante> variantesActivas = productoVarianteRepository
                .findByProductoIdAndActivaTrueOrderByOrdenAscIdAsc(producto.getId());

        if (variantesActivas.isEmpty()) {
            throw new ResourceNotFoundException("El producto no cuenta con variantes disponibles: " + idOrSlug);
        }

        List<ProductoImagen> imagenes = productoImagenRepository.findByProductoIdOrderByOrdenAscIdAsc(producto.getId());
        List<String> imgUrls = imagenes.stream()
                .map(img -> "/api/public/productos/" + producto.getId() + "/imagenes/" + img.getId() + "?v=" + img.getActualizadoEn().toEpochMilli())
                .toList();

        List<VariantePublicoDto> varDtos = variantesActivas.stream()
                .map(v -> new VariantePublicoDto(
                        v.getId(),
                        v.getNombre(),
                        v.getPrecio().longValue(),
                        v.getStock()
                ))
                .toList();

        List<String> incluyeList = (producto.getIncluye() == null || producto.getIncluye().isBlank())
                ? List.of()
                : Arrays.stream(producto.getIncluye().split("\\r?\\n"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();

        return new ProductoPublicoDto(
                producto.getId(),
                producto.getNombre(),
                producto.getCategoria().getId(),
                producto.getLema(),
                producto.getDescripcion() != null ? producto.getDescripcion() : "",
                incluyeList,
                producto.getGarantia(),
                producto.getEtiquetaVariante(),
                imgUrls,
                varDtos
        );
    }

    private Long parseLongOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
