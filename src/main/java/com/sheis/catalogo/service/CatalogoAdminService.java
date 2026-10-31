package com.sheis.catalogo.service;

import com.sheis.catalogo.domain.*;
import com.sheis.catalogo.exception.BadRequestException;
import com.sheis.catalogo.exception.ResourceConflictException;
import com.sheis.catalogo.exception.ResourceNotFoundException;
import com.sheis.catalogo.repository.*;
import com.sheis.catalogo.stock.StockProvider;
import com.sheis.catalogo.web.dto.admin.*;
import com.sheis.catalogo.web.dto.publico.CatalogoTextosDto;
import com.sheis.catalogo.web.dto.publico.FavoritoDto;
import com.sheis.catalogo.web.dto.publico.HorarioDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CatalogoAdminService {

    private final SkuCategoriaRepository skuCategoriaRepository;
    private final ServicioRepository servicioRepository;
    private final TarifaRepository tarifaRepository;
    private final ComboRepository comboRepository;
    private final DescuentoComboRepository descuentoComboRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final HorarioRepository horarioRepository;
    private final CatalogoConfigRepository catalogoConfigRepository;
    private final PricingService pricingService;
    private final StockProvider stockProvider;
    private final ImagenServicioStore imagenServicioStore;
    private final ImagenMetodoPagoStore imagenMetodoPagoStore;
    private final CategoriaProductoRepository categoriaProductoRepository;
    private final ProductoRepository productoRepository;
    private final ProductoVarianteRepository productoVarianteRepository;
    private final ProductoImagenRepository productoImagenRepository;
    private final ImagenProductoStore imagenProductoStore;

    public CatalogoAdminService(SkuCategoriaRepository skuCategoriaRepository,
                                ServicioRepository servicioRepository,
                                TarifaRepository tarifaRepository,
                                ComboRepository comboRepository,
                                DescuentoComboRepository descuentoComboRepository,
                                MetodoPagoRepository metodoPagoRepository,
                                HorarioRepository horarioRepository,
                                CatalogoConfigRepository catalogoConfigRepository,
                                PricingService pricingService,
                                StockProvider stockProvider,
                                ImagenServicioStore imagenServicioStore,
                                ImagenMetodoPagoStore imagenMetodoPagoStore,
                                CategoriaProductoRepository categoriaProductoRepository,
                                ProductoRepository productoRepository,
                                ProductoVarianteRepository productoVarianteRepository,
                                ProductoImagenRepository productoImagenRepository,
                                ImagenProductoStore imagenProductoStore) {
        this.skuCategoriaRepository = skuCategoriaRepository;
        this.servicioRepository = servicioRepository;
        this.tarifaRepository = tarifaRepository;
        this.comboRepository = comboRepository;
        this.descuentoComboRepository = descuentoComboRepository;
        this.metodoPagoRepository = metodoPagoRepository;
        this.horarioRepository = horarioRepository;
        this.catalogoConfigRepository = catalogoConfigRepository;
        this.pricingService = pricingService;
        this.stockProvider = stockProvider;
        this.imagenServicioStore = imagenServicioStore;
        this.imagenMetodoPagoStore = imagenMetodoPagoStore;
        this.categoriaProductoRepository = categoriaProductoRepository;
        this.productoRepository = productoRepository;
        this.productoVarianteRepository = productoVarianteRepository;
        this.productoImagenRepository = productoImagenRepository;
        this.imagenProductoStore = imagenProductoStore;
    }

    @Transactional(readOnly = true)
    public AdminCatalogoSnapshot obtenerSnapshot() {
        // Descuentos
        AdminDescuentosDto descuentos = obtenerDescuentos();

        // Mapa de precios base para combos
        List<SkuCategoria> todos = skuCategoriaRepository.findAllWithTarifas();
        Map<String, Instant> versionesImagenes = imagenServicioStore.obtenerVersionesPorCodigo();
        Map<String, Long> preciosBaseMap = new HashMap<>();
        Map<String, Boolean> agotadoMap = new HashMap<>();
        List<AdminServicioDto> serviciosDtos = new ArrayList<>();

        for (SkuCategoria s : todos) {
            Optional<Long> optBase = pricingService.calcularPrecioBase(s.getTarifas());
            Long precioBase = optBase.orElse(null);
            if (precioBase != null) {
                preciosBaseMap.put(s.getCodigo(), precioBase);
            }

            Integer stock = stockProvider.stockDe(s);
            boolean agotado = stock != null && stock == 0;
            agotadoMap.put(s.getCodigo(), agotado);

            List<AdminPlanDto> planes = s.getTarifas().stream()
                    .sorted(Comparator.comparingInt(Tarifa::getDias).thenComparingInt(Tarifa::getCantidadPerfiles))
                    .map(t -> new AdminPlanDto(t.getDias(), t.getCantidadPerfiles(), t.getPrecio(), t.getActivo()))
                    .toList();

            Instant imgActualizada = versionesImagenes.get(s.getCodigo());
            String imagenUrl = imgActualizada != null
                    ? "/api/public/servicios/" + s.getCodigo() + "/imagen?v=" + imgActualizada.toEpochMilli()
                    : null;

            serviciosDtos.add(new AdminServicioDto(
                    s.getCodigo(),
                    s.getNombre(),
                    s.getModalidad(),
                    s.getInicial(),
                    s.getColor(),
                    s.getLogoUrl(),
                    imagenUrl,
                    s.getEtiqueta(),
                    s.getDescripcion(),
                    s.getIncluye(),
                    s.getRecomendaciones(),
                    s.getVisible(),
                    s.getOrden(),
                    s.getStockManual(),
                    stock,
                    agotado,
                    precioBase,
                    planes,
                    s.getLema(),
                    s.getFavoritoOrden()
            ));
        }

        // Combos
        Map<Integer, Long> descPorCantidad = descuentos.porCantidad().entrySet().stream()
                .collect(Collectors.toMap(e -> Integer.parseInt(e.getKey()), Map.Entry::getValue));

        List<AdminComboDto> combosDtos = comboRepository.findAllWithServicios().stream()
                .map(c -> {
                    List<String> codigos = c.getServicios().stream().map(SkuCategoria::getCodigo).toList();
                    List<Long> precios = codigos.stream()
                            .map(cod -> preciosBaseMap.getOrDefault(cod, 0L))
                            .toList();

                    PricingService.ComboPricing p = pricingService.calcularPrecioCombo(precios, descPorCantidad, descuentos.extraDesdeSexto());
                    boolean agotado = codigos.stream().anyMatch(cod -> Boolean.TRUE.equals(agotadoMap.get(cod)));

                    return new AdminComboDto(
                            c.getId(),
                            c.getEtiqueta(),
                            c.getActivo(),
                            c.getOrden(),
                            codigos,
                            p.precioNormal(),
                            p.descuento(),
                            p.precio(),
                            agotado,
                            c.getVisible() != null ? c.getVisible() : (c.getActivo() != null ? c.getActivo() : true),
                            c.getEtiqueta(),
                            c.getFavoritoOrden()
                    );
                })
                .toList();

        AdminAjustesDto ajustes = obtenerAjustes();

        // Favoritos ordenados para el panel de administración
        record TempFav(String tipo, String ref, int orden) {}
        List<TempFav> tempFavs = new ArrayList<>();
        for (SkuCategoria s : todos) {
            if (s.getFavoritoOrden() != null) {
                tempFavs.add(new TempFav("servicio", s.getCodigo(), s.getFavoritoOrden()));
            }
        }
        for (AdminComboDto c : combosDtos) {
            if (c.favoritoOrden() != null && c.id() != null) {
                tempFavs.add(new TempFav("combo", String.valueOf(c.id()), c.favoritoOrden()));
            }
        }
        tempFavs.sort(Comparator.comparingInt(TempFav::orden));
        List<FavoritoDto> favoritos = tempFavs.stream()
                .map(f -> new FavoritoDto(f.tipo(), f.ref()))
                .toList();

        // Categorías de producto
        List<CategoriaProducto> categoriasEnt = categoriaProductoRepository.findAllByOrderByOrdenAscIdAsc();
        List<AdminCategoriaProductoDto> categoriasProducto = categoriasEnt.stream()
                .map(cp -> new AdminCategoriaProductoDto(
                        cp.getId(),
                        cp.getNombre(),
                        cp.getOrden(),
                        cp.getVisible(),
                        productoRepository.countByCategoriaId(cp.getId())
                ))
                .toList();

        // Productos físicos
        List<Producto> productosEnt = productoRepository.findAllByOrderByOrdenAscIdAsc();
        List<AdminProductoDto> productos = productosEnt.stream()
                .map(this::mapToAdminProductoDto)
                .toList();

        return new AdminCatalogoSnapshot(serviciosDtos, combosDtos, descuentos, ajustes, favoritos, categoriasProducto, productos);
    }

    // ==================== SERVICIOS ====================

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public AdminServicioDto crearServicio(CrearServicioRequest req) {
        String codigo = req.codigo() != null && !req.codigo().isBlank()
                ? req.codigo().trim().toLowerCase()
                : generarSlug(req.nombre());

        if (skuCategoriaRepository.existsByCodigo(codigo)) {
            throw new ResourceConflictException("El código de servicio '" + codigo + "' ya existe");
        }

        Servicio servicio = servicioRepository.findByNombre(req.nombre())
                .orElseGet(() -> servicioRepository.save(Servicio.builder().nombre(req.nombre()).build()));

        String lema = req.lema() != null ? req.lema().trim() : null;
        if (lema != null && lema.length() > 30) {
            throw new BadRequestException("El lema del servicio no puede superar los 30 caracteres");
        }

        SkuCategoria s = SkuCategoria.builder()
                .servicio(servicio)
                .codigo(codigo)
                .nombre(req.nombre().trim())
                .modalidad(req.modalidad() != null && !req.modalidad().isBlank() ? req.modalidad().trim() : "Perfil")
                .inicial(req.inicial().trim().toUpperCase())
                .color(req.color().trim().toUpperCase())
                .etiqueta(req.etiqueta() != null ? req.etiqueta().trim() : "")
                .descripcion(req.descripcion() != null ? req.descripcion().trim() : "")
                .incluye(req.incluye() != null ? req.incluye() : new ArrayList<>())
                .recomendaciones(req.recomendaciones() != null ? req.recomendaciones() : new ArrayList<>())
                .logoUrl(req.logoUrl() != null && !req.logoUrl().isBlank() ? req.logoUrl().trim() : null)
                .orden(req.orden() != null ? req.orden() : 0)
                .visible(req.visible() != null ? req.visible() : true)
                .stockManual(req.stockManual())
                .lema(lema != null && !lema.isEmpty() ? lema : null)
                .favoritoOrden(req.favoritoOrden())
                .build();

        s = skuCategoriaRepository.save(s);

        // Crear plan inicial inactivo 30 días, 1 perfil, precio 0
        Tarifa inicial = Tarifa.builder()
                .skuCategoria(s)
                .dias(30)
                .cantidadPerfiles(1)
                .precio(BigDecimal.ZERO)
                .activo(false)
                .build();

        tarifaRepository.save(inicial);
        s.getTarifas().add(inicial);

        return mapToAdminDto(s);
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public AdminServicioDto actualizarServicio(String codigo, ActualizarServicioRequest req) {
        SkuCategoria s = skuCategoriaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("El servicio '" + codigo + "' no existe"));

        s.setNombre(req.nombre().trim());
        s.setInicial(req.inicial().trim().toUpperCase());
        s.setColor(req.color().trim().toUpperCase());
        s.setEtiqueta(req.etiqueta() != null ? req.etiqueta().trim() : "");
        s.setDescripcion(req.descripcion() != null ? req.descripcion().trim() : "");
        s.setIncluye(req.incluye() != null ? req.incluye() : new ArrayList<>());
        s.setRecomendaciones(req.recomendaciones() != null ? req.recomendaciones() : new ArrayList<>());
        s.setLogoUrl(req.logoUrl() != null && !req.logoUrl().isBlank() ? req.logoUrl().trim() : null);
        if (req.orden() != null) {
            s.setOrden(req.orden());
        }
        if (req.lema() != null) {
            String lema = req.lema().trim();
            if (lema.length() > 30) {
                throw new BadRequestException("El lema del servicio no puede superar los 30 caracteres");
            }
            s.setLema(lema.isEmpty() ? null : lema);
        }
        if (req.favoritoOrden() != null) {
            s.setFavoritoOrden(req.favoritoOrden());
        }

        s = skuCategoriaRepository.save(s);
        return mapToAdminDto(s);
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public void cambiarVisible(String codigo, boolean visible) {
        SkuCategoria s = skuCategoriaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("El servicio '" + codigo + "' no existe"));
        s.setVisible(visible);
        skuCategoriaRepository.save(s);
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public void cambiarStock(String codigo, Integer stock) {
        SkuCategoria s = skuCategoriaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("El servicio '" + codigo + "' no existe"));
        s.setStockManual(stock);
        skuCategoriaRepository.save(s);
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public EliminarServicioResponse eliminarServicio(String codigo) {
        SkuCategoria s = skuCategoriaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("El servicio '" + codigo + "' no existe"));

        // Quitar de combos y verificar combos afectados
        List<Combo> combos = comboRepository.findAllWithServicios();
        List<Long> combosAfectados = new ArrayList<>();

        for (Combo c : combos) {
            if (c.getServicios().removeIf(it -> it.getCodigo().equals(codigo))) {
                if (c.getServicios().size() < 2) {
                    c.setActivo(false);
                    combosAfectados.add(c.getId());
                }
                comboRepository.save(c);
            }
        }

        skuCategoriaRepository.delete(s);
        return new EliminarServicioResponse(
                "Servicio '" + codigo + "' eliminado exitosamente",
                combosAfectados
        );
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public ImagenServicioStore.ImagenGuardada guardarImagen(String codigo, byte[] bytes, String declaredContentType) {
        if (!skuCategoriaRepository.existsByCodigo(codigo)) {
            throw new ResourceNotFoundException("El servicio '" + codigo + "' no existe");
        }
        String detectedContentType = ImageValidator.validarYDetectarContentType(bytes, declaredContentType);
        return imagenServicioStore.guardar(codigo, detectedContentType, bytes);
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public void eliminarImagen(String codigo) {
        imagenServicioStore.eliminar(codigo);
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public ImagenMetodoPagoStore.ImagenGuardada guardarImagenMetodoPago(String id, byte[] bytes, String declaredContentType) {
        if (!metodoPagoRepository.existsById(id)) {
            throw new ResourceNotFoundException("El método de pago '" + id + "' no existe");
        }
        String detectedContentType = ImageValidator.validarYDetectarContentType(bytes, declaredContentType);
        return imagenMetodoPagoStore.guardar(id, detectedContentType, bytes);
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public void eliminarImagenMetodoPago(String id) {
        imagenMetodoPagoStore.eliminar(id);
    }

    // ==================== PLANES ====================

    @Transactional(readOnly = true)
    public List<AdminPlanDto> obtenerPlanes(String codigo) {
        if (!skuCategoriaRepository.existsByCodigo(codigo)) {
            throw new ResourceNotFoundException("El servicio '" + codigo + "' no existe");
        }

        return tarifaRepository.findBySkuCategoriaCodigoOrderByDiasAscCantidadPerfilesAsc(codigo).stream()
                .map(t -> new AdminPlanDto(t.getDias(), t.getCantidadPerfiles(), t.getPrecio(), t.getActivo()))
                .toList();
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public List<AdminPlanDto> reemplazarPlanes(String codigo, List<AdminPlanDto> planes) {
        SkuCategoria s = skuCategoriaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("El servicio '" + codigo + "' no existe"));

        if (planes == null) {
            planes = List.of();
        }

        // Validaciones: unicidad (dias + perfiles), dias>0, perfiles>0, precio>=0, activo=>precio>0
        Set<String> uniqueCheck = new HashSet<>();
        for (AdminPlanDto p : planes) {
            if (p.dias() == null || p.dias() <= 0) {
                throw new BadRequestException("Los días de cada plan deben ser mayores a 0");
            }
            if (p.perfiles() == null || p.perfiles() <= 0) {
                throw new BadRequestException("La cantidad de perfiles debe ser mayor a 0");
            }
            if (p.precio() == null || p.precio().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("El precio debe ser mayor o igual a 0");
            }
            boolean esActivo = Boolean.TRUE.equals(p.activo());
            if (esActivo && p.precio().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Un plan activo debe tener un precio mayor a 0");
            }

            String key = p.dias() + "-" + p.perfiles();
            if (!uniqueCheck.add(key)) {
                throw new BadRequestException("No se permiten combinaciones duplicadas de días y perfiles: " + p.dias() + " días, " + p.perfiles() + " perfil(es)");
            }
        }

        // Borrar existentes, flush y luego insertar
        tarifaRepository.deleteBySkuCategoriaCodigo(codigo);
        tarifaRepository.flush();

        List<Tarifa> nuevas = planes.stream()
                .map(dto -> Tarifa.builder()
                        .skuCategoria(s)
                        .dias(dto.dias())
                        .cantidadPerfiles(dto.perfiles())
                        .precio(dto.precio())
                        .activo(dto.activo() != null ? dto.activo() : true)
                        .build())
                .toList();

        tarifaRepository.saveAll(nuevas);

        return nuevas.stream()
                .sorted(Comparator.comparingInt(Tarifa::getDias).thenComparingInt(Tarifa::getCantidadPerfiles))
                .map(t -> new AdminPlanDto(t.getDias(), t.getCantidadPerfiles(), t.getPrecio(), t.getActivo()))
                .toList();
    }

    // ==================== COMBOS ====================

    @Transactional(readOnly = true)
    public List<AdminComboDto> obtenerCombos() {
        AdminCatalogoSnapshot snap = obtenerSnapshot();
        return snap.combos();
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public List<AdminComboDto> reemplazarCombos(List<AdminComboInputDto> inputs) {
        if (inputs == null) {
            inputs = List.of();
        }

        // Validar cada combo
        for (AdminComboInputDto in : inputs) {
            if (in.servicios() == null || in.servicios().size() < 2) {
                throw new BadRequestException("Cada combo debe incluir al menos 2 servicios distintos");
            }
            Set<String> distinct = new HashSet<>(in.servicios());
            if (distinct.size() != in.servicios().size()) {
                throw new BadRequestException("Un combo no puede contener servicios repetidos");
            }
            String badge = in.getEtiquetaEfectiva();
            if (badge.length() > 30) {
                throw new BadRequestException("La etiqueta/badge del combo no puede superar los 30 caracteres");
            }
        }

        Map<Long, Combo> existentesMap = comboRepository.findAll().stream()
                .collect(Collectors.toMap(Combo::getId, c -> c));

        Set<Long> idsConservados = new HashSet<>();
        List<Combo> aGuardar = new ArrayList<>();
        int defaultOrden = 1;

        for (AdminComboInputDto in : inputs) {
            List<SkuCategoria> servicios = skuCategoriaRepository.findByCodigoIn(in.servicios());
            if (servicios.size() != in.servicios().size()) {
                throw new BadRequestException("Uno o más códigos de servicio especificados en el combo no existen");
            }

            String etiqueta = in.getEtiquetaEfectiva();
            boolean visible = in.getVisibleEfectivo();
            int orden = in.orden() != null ? in.orden() : defaultOrden++;
            Integer favoritoOrden = in.favoritoOrden();

            Combo combo;
            if (in.id() != null && existentesMap.containsKey(in.id())) {
                combo = existentesMap.get(in.id());
                idsConservados.add(combo.getId());
                combo.setEtiqueta(etiqueta);
                combo.setActivo(visible);
                combo.setVisible(visible);
                combo.setOrden(orden);
                if (favoritoOrden != null) {
                    combo.setFavoritoOrden(favoritoOrden);
                }
                combo.setServicios(new ArrayList<>(servicios));
            } else {
                combo = Combo.builder()
                        .etiqueta(etiqueta)
                        .activo(visible)
                        .visible(visible)
                        .orden(orden)
                        .favoritoOrden(favoritoOrden)
                        .servicios(new ArrayList<>(servicios))
                        .build();
            }
            aGuardar.add(combo);
        }

        // Eliminar combos retirados de la lista (su borrado los quita de favoritos)
        for (Combo existente : existentesMap.values()) {
            if (!idsConservados.contains(existente.getId())) {
                comboRepository.delete(existente);
            }
        }
        comboRepository.flush();

        comboRepository.saveAll(aGuardar);
        comboRepository.flush();

        return obtenerCombos();
    }

    // ==================== DESCUENTOS ====================

    @Transactional(readOnly = true)
    public AdminDescuentosDto obtenerDescuentos() {
        List<DescuentoCombo> descList = descuentoComboRepository.findAllByOrderByCantidadServiciosAsc();
        Map<String, Long> porCantidad = descList.stream()
                .collect(Collectors.toMap(
                        d -> String.valueOf(d.getCantidadServicios()),
                        d -> d.getDescuento().longValue()
                ));

        long extra = catalogoConfigRepository.findByClave("descuento_extra_desde_6")
                .map(c -> {
                    try {
                        return Long.parseLong(c.getValor().trim());
                    } catch (NumberFormatException e) {
                        return 2000L;
                    }
                })
                .orElse(2000L);

        return new AdminDescuentosDto(porCantidad, extra);
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public AdminDescuentosDto actualizarDescuentos(AdminDescuentosDto dto) {
        if (dto.extraDesdeSexto() < 0) {
            throw new BadRequestException("El descuento extra desde el 6to servicio debe ser mayor o igual a 0");
        }

        if (dto.porCantidad() != null) {
            for (Map.Entry<String, Long> entry : dto.porCantidad().entrySet()) {
                int cant;
                try {
                    cant = Integer.parseInt(entry.getKey());
                } catch (NumberFormatException e) {
                    throw new BadRequestException("Clave de cantidad inválida: " + entry.getKey());
                }

                if (cant < 2) {
                    throw new BadRequestException("La cantidad mínima para descuento de combo es 2 servicios");
                }
                if (entry.getValue() < 0) {
                    throw new BadRequestException("El descuento no puede ser negativo");
                }

                DescuentoCombo dc = descuentoComboRepository.findById(cant)
                        .orElse(DescuentoCombo.builder().cantidadServicios(cant).build());
                dc.setDescuento(BigDecimal.valueOf(entry.getValue()));
                dc.setActivo(true);
                descuentoComboRepository.save(dc);
            }
        }

        CatalogoConfig cfg = catalogoConfigRepository.findByClave("descuento_extra_desde_6")
                .orElse(CatalogoConfig.builder().clave("descuento_extra_desde_6").build());
        cfg.setValor(String.valueOf(dto.extraDesdeSexto()));
        catalogoConfigRepository.save(cfg);

        return obtenerDescuentos();
    }

    // ==================== AJUSTES ====================

    @Transactional(readOnly = true)
    public AdminAjustesDto obtenerAjustes() {
        String whatsapp = catalogoConfigRepository.findByClave("whatsapp")
                .map(CatalogoConfig::getValor)
                .orElse("57 300 000 0000");

        String sobreNosotros = catalogoConfigRepository.findByClave("sobre_nosotros")
                .map(CatalogoConfig::getValor)
                .orElse("");

        List<HorarioDto> horarios = horarioRepository.findAllByOrderByOrdenAsc().stream()
                .map(h -> new HorarioDto(h.getDia(), h.getTexto()))
                .toList();

        Map<String, Instant> versionesImagenesMetodos = imagenMetodoPagoStore.obtenerVersionesPorId();

        List<AdminMetodoPagoDto> metodos = metodoPagoRepository.findAllByOrderByOrdenAsc().stream()
                .map(m -> {
                    Instant imgActualizada = versionesImagenesMetodos.get(m.getId());
                    String imagenUrl = imgActualizada != null
                            ? "/api/public/metodos-pago/" + m.getId() + "/imagen?v=" + imgActualizada.toEpochMilli()
                            : null;
                    return new AdminMetodoPagoDto(
                            m.getId(),
                            m.getNombre(),
                            m.getInicial(),
                            m.getColor(),
                            m.getEtiquetaDato(),
                            m.getDato(),
                            m.getTitular(),
                            m.getActivo(),
                            m.getOrden(),
                            imagenUrl
                    );
                })
                .toList();

        CatalogoTextosDto textos = new CatalogoTextosDto(
                obtenerConfig("banner_texto", "bannerTexto", "Elige tu mejor servicio y empieza tu maratón", true),
                obtenerConfig("titulo_favoritos", "tituloFavoritos", "Servicios favoritos", false),
                obtenerConfig("titulo_servicios", "tituloServicios", "Servicios", false),
                obtenerConfig("titulo_combos", "tituloCombos", "Combos", false),
                obtenerConfig("menu_favoritos", "menuFavoritos", "Servicios favoritos", false),
                obtenerConfig("menu_servicios", "menuServicios", "Servicios individuales", false),
                obtenerConfig("menu_combos", "menuCombos", "Combos con descuento", false),
                obtenerConfig("titulo_tecnologia", "tituloTecnologia", "Tecnología", false),
                obtenerConfig("menu_tecnologia", "menuTecnologia", "Tecnología", false),
                obtenerConfig("subtitulo_tecnologia", "subtituloTecnologia", "Audífonos, forros, cargadores y más. Envíos a todo el país.", true),
                obtenerConfig("ciudad_domicilio", "ciudadDomicilio", "", true),
                obtenerConfig("texto_domicilio", "textoDomicilio", "Te lo llevamos a la puerta de tu casa.", true),
                obtenerConfig("texto_envio_nacional", "textoEnvioNacional", "Por transportadora a cualquier ciudad. El costo te lo confirmamos por WhatsApp.", true),
                obtenerConfig("nota_tecnologia", "notaTecnologia", "Pide por WhatsApp. Domicilio en tu ciudad o envío nacional; el costo del envío te lo confirmamos en el chat.", true)
        );

        return new AdminAjustesDto(whatsapp, sobreNosotros, horarios, metodos, textos);
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public AdminAjustesDto actualizarAjustes(AdminAjustesDto dto) {
        if (dto.whatsapp() != null) {
            CatalogoConfig w = catalogoConfigRepository.findByClave("whatsapp")
                    .orElse(CatalogoConfig.builder().clave("whatsapp").build());
            w.setValor(dto.whatsapp().trim());
            catalogoConfigRepository.save(w);
        }

        if (dto.sobreNosotros() != null) {
            CatalogoConfig s = catalogoConfigRepository.findByClave("sobre_nosotros")
                    .orElse(CatalogoConfig.builder().clave("sobre_nosotros").build());
            s.setValor(dto.sobreNosotros().trim());
            catalogoConfigRepository.save(s);
        }

        if (dto.horario() != null) {
            horarioRepository.deleteAll();
            horarioRepository.flush();
            int o = 1;
            List<Horario> newHorarios = new ArrayList<>();
            for (HorarioDto h : dto.horario()) {
                newHorarios.add(Horario.builder()
                        .orden(o++)
                        .dia(h.dia())
                        .texto(h.texto())
                        .build());
            }
            horarioRepository.saveAll(newHorarios);
        }

        if (dto.metodosPago() != null) {
            for (AdminMetodoPagoDto m : dto.metodosPago()) {
                MetodoPago mp = metodoPagoRepository.findById(m.id())
                        .orElse(MetodoPago.builder().id(m.id()).build());

                mp.setNombre(m.nombre());
                mp.setInicial(m.inicial());
                mp.setColor(m.color());
                mp.setEtiquetaDato(m.etiquetaDato());
                mp.setDato(m.dato());
                mp.setTitular(m.titular());
                mp.setActivo(m.activo() != null ? m.activo() : true);
                if (m.orden() != null) {
                    mp.setOrden(m.orden());
                }
                metodoPagoRepository.save(mp);
            }
        }

        if (dto.textos() != null) {
            guardarTextoConfig("banner_texto", "bannerTexto", dto.textos().bannerTexto(), 90, "El texto de la banda");
            guardarTextoConfig("titulo_favoritos", "tituloFavoritos", dto.textos().tituloFavoritos(), 40, "El título de favoritos");
            guardarTextoConfig("titulo_servicios", "tituloServicios", dto.textos().tituloServicios(), 40, "El título de servicios");
            guardarTextoConfig("titulo_combos", "tituloCombos", dto.textos().tituloCombos(), 40, "El título de combos");
            guardarTextoConfig("menu_favoritos", "menuFavoritos", dto.textos().menuFavoritos(), 30, "El texto del menú de favoritos");
            guardarTextoConfig("menu_servicios", "menuServicios", dto.textos().menuServicios(), 30, "El texto del menú de servicios");
            guardarTextoConfig("menu_combos", "menuCombos", dto.textos().menuCombos(), 30, "El texto del menú de combos");
            guardarTextoConfig("titulo_tecnologia", "tituloTecnologia", dto.textos().tituloTecnologia(), 40, "El título de tecnología");
            guardarTextoConfig("menu_tecnologia", "menuTecnologia", dto.textos().menuTecnologia(), 30, "El texto del menú de tecnología");
            guardarTextoConfig("subtitulo_tecnologia", "subtituloTecnologia", dto.textos().subtituloTecnologia(), 120, "El subtítulo de tecnología");
            guardarTextoConfig("ciudad_domicilio", "ciudadDomicilio", dto.textos().ciudadDomicilio(), 50, "La ciudad de domicilio");
            guardarTextoConfig("texto_domicilio", "textoDomicilio", dto.textos().textoDomicilio(), 120, "El texto de domicilio");
            guardarTextoConfig("texto_envio_nacional", "textoEnvioNacional", dto.textos().textoEnvioNacional(), 120, "El texto de envío nacional");
            guardarTextoConfig("nota_tecnologia", "notaTecnologia", dto.textos().notaTecnologia(), 200, "La nota de tecnología");
        }

        return obtenerAjustes();
    }

    private String obtenerConfig(String snakeClave, String camelClave, String def, boolean permitirVacio) {
        return catalogoConfigRepository.findByClave(snakeClave)
                .or(() -> catalogoConfigRepository.findByClave(camelClave))
                .map(CatalogoConfig::getValor)
                .map(String::trim)
                .filter(v -> permitirVacio || !v.isEmpty())
                .orElse(def);
    }

    private void guardarTextoConfig(String snakeClave, String camelClave, String valor, int maxLen, String nombreCampo) {
        if (valor == null) return;
        String trimmed = valor.trim();
        if (trimmed.length() > maxLen) {
            throw new BadRequestException(nombreCampo + " no puede superar los " + maxLen + " caracteres");
        }
        CatalogoConfig cfg = catalogoConfigRepository.findByClave(snakeClave)
                .or(() -> catalogoConfigRepository.findByClave(camelClave))
                .orElse(CatalogoConfig.builder().clave(snakeClave).build());
        cfg.setValor(trimmed);
        catalogoConfigRepository.save(cfg);
    }

    // ==================== FAVORITOS ====================

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public List<FavoritoDto> actualizarFavoritos(List<FavoritoItemRequest> items) {
        if (items == null) {
            items = List.of();
        }

        if (items.size() > 12) {
            throw new BadRequestException("No se pueden configurar más de 12 favoritos");
        }

        Set<String> seen = new HashSet<>();
        List<FavoritoDto> resultado = new ArrayList<>();

        // 1. Validaciones previas
        for (FavoritoItemRequest item : items) {
            if (item.tipo() == null || item.tipo().isBlank()) {
                throw new BadRequestException("El tipo de favorito es obligatorio");
            }
            if (item.ref() == null || item.ref().isBlank()) {
                throw new BadRequestException("La referencia del favorito es obligatoria");
            }
            String tipo = item.tipo().trim().toLowerCase();
            String ref = item.ref().trim();

            if (!"servicio".equals(tipo) && !"combo".equals(tipo)) {
                throw new BadRequestException("Tipo de favorito desconocido: '" + tipo + "'. Debe ser 'servicio' o 'combo'");
            }

            String uniqueKey = tipo + ":" + ref;
            if (!seen.add(uniqueKey)) {
                throw new BadRequestException("No se permiten favoritos duplicados: " + ref);
            }

            if ("servicio".equals(tipo)) {
                if (!skuCategoriaRepository.existsByCodigo(ref)) {
                    throw new BadRequestException("El servicio con código '" + ref + "' no existe");
                }
            } else {
                long comboId;
                try {
                    comboId = Long.parseLong(ref);
                } catch (NumberFormatException e) {
                    throw new BadRequestException("La referencia del combo debe ser un ID numérico válido: '" + ref + "'");
                }
                if (!comboRepository.existsById(comboId)) {
                    throw new BadRequestException("El combo con ID '" + ref + "' no existe");
                }
            }
        }

        // 2. Limpiar todos los favoritos existentes
        skuCategoriaRepository.clearAllFavoritoOrden();
        comboRepository.clearAllFavoritoOrden();
        skuCategoriaRepository.flush();
        comboRepository.flush();

        // 3. Asignar nuevo orden 1..N
        int orden = 1;
        for (FavoritoItemRequest item : items) {
            String tipo = item.tipo().trim().toLowerCase();
            String ref = item.ref().trim();

            if ("servicio".equals(tipo)) {
                SkuCategoria s = skuCategoriaRepository.findByCodigo(ref)
                        .orElseThrow(() -> new BadRequestException("El servicio con código '" + ref + "' no existe"));
                s.setFavoritoOrden(orden++);
                skuCategoriaRepository.save(s);
            } else {
                long comboId = Long.parseLong(ref);
                Combo c = comboRepository.findById(comboId)
                        .orElseThrow(() -> new BadRequestException("El combo con ID '" + ref + "' no existe"));
                c.setFavoritoOrden(orden++);
                comboRepository.save(c);
            }

            resultado.add(new FavoritoDto(tipo, ref));
        }

        return resultado;
    }

    private AdminServicioDto mapToAdminDto(SkuCategoria s) {
        Optional<Long> optBase = pricingService.calcularPrecioBase(s.getTarifas());
        Long precioBase = optBase.orElse(null);
        Integer stock = stockProvider.stockDe(s);
        boolean agotado = stock != null && stock == 0;

        List<AdminPlanDto> planes = s.getTarifas().stream()
                .sorted(Comparator.comparingInt(Tarifa::getDias).thenComparingInt(Tarifa::getCantidadPerfiles))
                .map(t -> new AdminPlanDto(t.getDias(), t.getCantidadPerfiles(), t.getPrecio(), t.getActivo()))
                .toList();

        Instant imgActualizada = imagenServicioStore.obtener(s.getCodigo())
                .map(ImagenServicioStore.ImagenData::actualizadoEn)
                .orElse(null);
        String imagenUrl = imgActualizada != null
                ? "/api/public/servicios/" + s.getCodigo() + "/imagen?v=" + imgActualizada.toEpochMilli()
                : null;

        return new AdminServicioDto(
                s.getCodigo(),
                s.getNombre(),
                s.getModalidad(),
                s.getInicial(),
                s.getColor(),
                s.getLogoUrl(),
                imagenUrl,
                s.getEtiqueta(),
                s.getDescripcion(),
                s.getIncluye(),
                s.getRecomendaciones(),
                s.getVisible(),
                s.getOrden(),
                s.getStockManual(),
                stock,
                agotado,
                precioBase,
                planes,
                s.getLema(),
                s.getFavoritoOrden()
        );
    }

    private String generarSlug(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "svc-" + System.currentTimeMillis();
        }
        String normalized = Normalizer.normalize(nombre, Normalizer.Form.NFD);
        String slug = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (slug.length() > 20) {
            slug = slug.substring(0, 20);
        }

        String candidato = slug;
        int i = 1;
        while (skuCategoriaRepository.existsByCodigo(candidato)) {
            candidato = slug + "-" + i++;
        }
        return candidato;
    }

    // ==================== PRODUCTOS Y CATEGORÍAS ====================

    private AdminProductoDto mapToAdminProductoDto(Producto p) {
        List<AdminProductoImagenDto> imagenes = productoImagenRepository.findByProductoIdOrderByOrdenAscIdAsc(p.getId()).stream()
                .map(img -> new AdminProductoImagenDto(
                        img.getId(),
                        "/api/public/productos/" + p.getId() + "/imagenes/" + img.getId() + "?v=" + img.getActualizadoEn().toEpochMilli(),
                        img.getOrden()
                ))
                .toList();

        List<AdminProductoVarianteDto> variantes = productoVarianteRepository.findByProductoIdOrderByOrdenAscIdAsc(p.getId()).stream()
                .map(v -> new AdminProductoVarianteDto(
                        v.getId(),
                        v.getNombre(),
                        v.getSku(),
                        v.getPrecio(),
                        v.getStock(),
                        v.getActiva(),
                        v.getOrden()
                ))
                .toList();

        return new AdminProductoDto(
                p.getId(),
                p.getCategoria().getId(),
                p.getCategoria().getNombre(),
                p.getNombre(),
                p.getDescripcion(),
                p.getIncluye(),
                p.getGarantia(),
                p.getLema(),
                p.getEtiquetaVariante(),
                p.getOrden(),
                p.getVisible(),
                imagenes,
                variantes
        );
    }

    // ==================== CATEGORÍAS DE PRODUCTO ====================

    @Transactional(readOnly = true)
    public List<AdminCategoriaProductoDto> obtenerCategoriasProducto() {
        return categoriaProductoRepository.findAllByOrderByOrdenAscIdAsc().stream()
                .map(cp -> new AdminCategoriaProductoDto(
                        cp.getId(),
                        cp.getNombre(),
                        cp.getOrden(),
                        cp.getVisible(),
                        productoRepository.countByCategoriaId(cp.getId())
                ))
                .toList();
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public AdminCategoriaProductoDto crearCategoriaProducto(CrearCategoriaProductoRequest req) {
        String nombre = req.nombre() != null ? req.nombre().trim() : "";
        if (nombre.isBlank()) {
            throw new BadRequestException("El nombre de la categoría es obligatorio");
        }
        if (nombre.length() > 40) {
            throw new BadRequestException("El nombre de la categoría no puede superar los 40 caracteres");
        }
        if (categoriaProductoRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ResourceConflictException("Ya existe una categoría con el nombre '" + nombre + "'");
        }

        CategoriaProducto cp = new CategoriaProducto(
                nombre,
                req.orden() != null ? req.orden() : 1,
                req.visible() != null ? req.visible() : true
        );
        CategoriaProducto guardada = categoriaProductoRepository.save(cp);
        return new AdminCategoriaProductoDto(guardada.getId(), guardada.getNombre(), guardada.getOrden(), guardada.getVisible(), 0L);
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public AdminCategoriaProductoDto actualizarCategoriaProducto(Long id, ActualizarCategoriaProductoRequest req) {
        CategoriaProducto cp = categoriaProductoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));

        String nombre = req.nombre() != null ? req.nombre().trim() : "";
        if (nombre.isBlank()) {
            throw new BadRequestException("El nombre de la categoría es obligatorio");
        }
        if (nombre.length() > 40) {
            throw new BadRequestException("El nombre de la categoría no puede superar los 40 caracteres");
        }
        if (categoriaProductoRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ResourceConflictException("Ya existe otra categoría con el nombre '" + nombre + "'");
        }

        cp.setNombre(nombre);
        if (req.orden() != null) {
            cp.setOrden(req.orden());
        }
        if (req.visible() != null) {
            cp.setVisible(req.visible());
        }
        cp.setActualizadoEn(Instant.now());
        CategoriaProducto guardada = categoriaProductoRepository.save(cp);
        long count = productoRepository.countByCategoriaId(guardada.getId());
        return new AdminCategoriaProductoDto(guardada.getId(), guardada.getNombre(), guardada.getOrden(), guardada.getVisible(), count);
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public void eliminarCategoriaProducto(Long id) {
        CategoriaProducto cp = categoriaProductoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));

        long count = productoRepository.countByCategoriaId(id);
        if (count > 0) {
            throw new ResourceConflictException("No se puede eliminar la categoría porque tiene " + count + " producto(s) asociado(s)");
        }

        categoriaProductoRepository.delete(cp);
    }

    // ==================== PRODUCTOS FÍSICOS ====================

    @Transactional(readOnly = true)
    public List<AdminProductoDto> obtenerProductos() {
        return productoRepository.findAllByOrderByOrdenAscIdAsc().stream()
                .map(this::mapToAdminProductoDto)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public AdminProductoDto crearProducto(CrearProductoRequest req) {
        CategoriaProducto categoria = categoriaProductoRepository.findById(req.categoriaId())
                .orElseThrow(() -> new BadRequestException("La categoría con ID " + req.categoriaId() + " no existe"));

        String nombre = req.nombre() != null ? req.nombre().trim() : "";
        if (nombre.isBlank()) {
            throw new BadRequestException("El nombre del producto es obligatorio");
        }
        if (nombre.length() > 80) {
            throw new BadRequestException("El nombre del producto no puede superar los 80 caracteres");
        }

        boolean visible = req.visible() != null ? req.visible() : true;

        List<AdminVarianteInputDto> variantesReq = req.variantes() != null ? req.variantes() : List.of();
        if (visible && (variantesReq.isEmpty() || variantesReq.stream().noneMatch(v -> v.activa() == null || Boolean.TRUE.equals(v.activa())))) {
            throw new BadRequestException("No se puede publicar un producto sin al menos una variante activa");
        }

        String lema = req.lema() != null ? req.lema().trim() : null;
        if (lema != null && lema.length() > 30) {
            throw new BadRequestException("El lema del producto no puede superar los 30 caracteres");
        }

        String etiquetaVariante = req.etiquetaVariante() != null ? req.etiquetaVariante().trim() : "Opción";
        if (etiquetaVariante.length() > 30) {
            throw new BadRequestException("La etiqueta de variante no puede superar los 30 caracteres");
        }

        Producto p = new Producto(
                categoria,
                nombre,
                req.descripcion() != null ? req.descripcion().trim() : null,
                req.incluye() != null ? req.incluye().trim() : null,
                req.garantia() != null ? req.garantia().trim() : null,
                lema,
                etiquetaVariante,
                req.orden() != null ? req.orden() : 1,
                visible
        );
        Producto guardado = productoRepository.save(p);

        if (!variantesReq.isEmpty()) {
            guardarVariantesInterno(guardado, variantesReq);
        }

        return mapToAdminProductoDto(guardado);
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public AdminProductoDto actualizarProducto(Long id, ActualizarProductoRequest req) {
        Producto p = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        CategoriaProducto categoria = categoriaProductoRepository.findById(req.categoriaId())
                .orElseThrow(() -> new BadRequestException("La categoría con ID " + req.categoriaId() + " no existe"));

        String nombre = req.nombre() != null ? req.nombre().trim() : "";
        if (nombre.isBlank()) {
            throw new BadRequestException("El nombre del producto es obligatorio");
        }
        if (nombre.length() > 80) {
            throw new BadRequestException("El nombre del producto no puede superar los 80 caracteres");
        }

        boolean nuevoVisible = req.visible() != null ? req.visible() : p.getVisible();
        if (nuevoVisible) {
            boolean tieneActiva = productoVarianteRepository.findByProductoIdAndActivaTrueOrderByOrdenAscIdAsc(id)
                    .stream().findAny().isPresent();
            if (!tieneActiva) {
                throw new BadRequestException("No se puede publicar un producto sin al menos una variante activa");
            }
        }

        String lema = req.lema() != null ? req.lema().trim() : null;
        if (lema != null && lema.length() > 30) {
            throw new BadRequestException("El lema del producto no puede superar los 30 caracteres");
        }

        String etiquetaVariante = req.etiquetaVariante() != null ? req.etiquetaVariante().trim() : "Opción";
        if (etiquetaVariante.length() > 30) {
            throw new BadRequestException("La etiqueta de variante no puede superar los 30 caracteres");
        }

        p.setCategoria(categoria);
        p.setNombre(nombre);
        p.setDescripcion(req.descripcion() != null ? req.descripcion().trim() : null);
        p.setIncluye(req.incluye() != null ? req.incluye().trim() : null);
        p.setGarantia(req.garantia() != null ? req.garantia().trim() : null);
        p.setLema(lema);
        p.setEtiquetaVariante(etiquetaVariante);
        if (req.orden() != null) {
            p.setOrden(req.orden());
        }
        p.setVisible(nuevoVisible);
        p.setActualizadoEn(Instant.now());

        Producto guardado = productoRepository.save(p);
        return mapToAdminProductoDto(guardado);
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public void cambiarVisibleProducto(Long id, boolean visible) {
        Producto p = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        if (visible) {
            boolean tieneActiva = productoVarianteRepository.findByProductoIdAndActivaTrueOrderByOrdenAscIdAsc(id)
                    .stream().findAny().isPresent();
            if (!tieneActiva) {
                throw new BadRequestException("No se puede publicar un producto sin al menos una variante activa");
            }
        }

        p.setVisible(visible);
        p.setActualizadoEn(Instant.now());
        productoRepository.save(p);
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public void eliminarProducto(Long id) {
        Producto p = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
        productoRepository.delete(p);
    }

    // ==================== VARIANTES ====================

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public List<AdminProductoVarianteDto> reemplazarVariantes(Long productoId, List<AdminVarianteInputDto> variantesReq) {
        Producto p = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + productoId));

        if (variantesReq == null || variantesReq.isEmpty()) {
            if (Boolean.TRUE.equals(p.getVisible())) {
                throw new BadRequestException("Un producto visible no puede quedarse sin variantes activas");
            }
            productoVarianteRepository.deleteAll(p.getVariantes());
            p.getVariantes().clear();
            return List.of();
        }

        if (Boolean.TRUE.equals(p.getVisible())) {
            boolean algunaActiva = variantesReq.stream()
                    .anyMatch(v -> v.activa() == null || Boolean.TRUE.equals(v.activa()));
            if (!algunaActiva) {
                throw new BadRequestException("Un producto visible no puede quedarse sin al menos una variante activa");
            }
        }

        return guardarVariantesInterno(p, variantesReq);
    }

    private List<AdminProductoVarianteDto> guardarVariantesInterno(Producto producto, List<AdminVarianteInputDto> variantesReq) {
        Set<String> seenNames = new HashSet<>();
        int orden = 1;

        List<ProductoVariante> actuales = productoVarianteRepository.findByProductoIdOrderByOrdenAscIdAsc(producto.getId());
        Map<Long, ProductoVariante> actualMap = actuales.stream().collect(Collectors.toMap(ProductoVariante::getId, v -> v));
        List<ProductoVariante> aGuardar = new ArrayList<>();
        Set<Long> conservadasIds = new HashSet<>();

        for (AdminVarianteInputDto input : variantesReq) {
            String vNombre = input.nombre() != null ? input.nombre().trim() : "";
            if (vNombre.isBlank()) {
                throw new BadRequestException("El nombre de la variante es obligatorio");
            }
            if (!seenNames.add(vNombre.toLowerCase())) {
                throw new BadRequestException("No puede haber variantes con el mismo nombre en el producto: '" + vNombre + "'");
            }

            if (input.precio() == null || input.precio().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("El precio de la variante debe ser mayor a 0");
            }
            int stock = input.stock() != null ? input.stock() : 0;
            if (stock < 0) {
                throw new BadRequestException("El stock de la variante no puede ser negativo");
            }

            String sku = input.sku() != null ? input.sku().trim() : "";
            if (sku.isBlank()) {
                sku = generarSkuVariante(producto.getNombre(), vNombre, input.id());
            } else {
                if (sku.length() > 50) {
                    throw new BadRequestException("El SKU no puede superar los 50 caracteres");
                }
                if (input.id() != null) {
                    if (productoVarianteRepository.existsBySkuIgnoreCaseAndIdNot(sku, input.id())) {
                        throw new ResourceConflictException("El SKU '" + sku + "' ya está registrado en otra variante");
                    }
                } else {
                    if (productoVarianteRepository.existsBySkuIgnoreCase(sku)) {
                        throw new ResourceConflictException("El SKU '" + sku + "' ya está registrado");
                    }
                }
            }

            ProductoVariante variante;
            if (input.id() != null && actualMap.containsKey(input.id())) {
                variante = actualMap.get(input.id());
                variante.setNombre(vNombre);
                variante.setSku(sku);
                variante.setPrecio(input.precio());
                variante.setStock(stock);
                variante.setActiva(input.activa() != null ? input.activa() : true);
                variante.setOrden(input.orden() != null ? input.orden() : orden++);
                variante.setActualizadoEn(Instant.now());
                conservadasIds.add(input.id());
            } else {
                variante = new ProductoVariante(
                        producto,
                        vNombre,
                        sku,
                        input.precio(),
                        stock,
                        input.activa() != null ? input.activa() : true,
                        input.orden() != null ? input.orden() : orden++
                );
            }
            aGuardar.add(variante);
        }

        // Eliminar las que ya no están en la lista
        for (ProductoVariante act : actuales) {
            if (!conservadasIds.contains(act.getId())) {
                producto.getVariantes().remove(act);
                productoVarianteRepository.delete(act);
            }
        }

        productoVarianteRepository.saveAllAndFlush(aGuardar);

        return productoVarianteRepository.findByProductoIdOrderByOrdenAscIdAsc(producto.getId()).stream()
                .map(v -> new AdminProductoVarianteDto(
                        v.getId(),
                        v.getNombre(),
                        v.getSku(),
                        v.getPrecio(),
                        v.getStock(),
                        v.getActiva(),
                        v.getOrden()
                ))
                .toList();
    }

    @Transactional
    @CacheEvict(value = "catalogo", allEntries = true)
    public void cambiarStockVariante(Long productoId, Long varianteId, Integer stock) {
        if (stock == null || stock < 0) {
            throw new BadRequestException("El stock debe ser un número entero mayor o igual a 0");
        }
        ProductoVariante pv = productoVarianteRepository.findById(varianteId)
                .orElseThrow(() -> new ResourceNotFoundException("Variante no encontrada con ID: " + varianteId));
        if (!pv.getProducto().getId().equals(productoId)) {
            throw new BadRequestException("La variante " + varianteId + " no pertenece al producto " + productoId);
        }
        pv.setStock(stock);
        pv.setActualizadoEn(Instant.now());
        productoVarianteRepository.save(pv);
    }

    private String generarSkuVariante(String productoNombre, String varianteNombre, Long varianteId) {
        String pPart = slugPart(productoNombre, 3);
        String vPart = slugPart(varianteNombre, 3);
        String base = pPart + "-" + vPart;

        String sku = base + "-" + (varianteId != null ? varianteId : (System.currentTimeMillis() % 100000));
        int i = 1;
        while (productoVarianteRepository.existsBySkuIgnoreCase(sku)) {
            sku = base + "-" + (System.currentTimeMillis() % 100000) + "-" + i++;
        }
        return sku.toUpperCase();
    }

    private String slugPart(String text, int len) {
        if (text == null || text.isBlank()) return "VAR";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-zA-Z0-9]", "")
                .toUpperCase();
        return normalized.length() <= len ? normalized : normalized.substring(0, len);
    }

    // ==================== IMÁGENES DE PRODUCTO ====================

    @Transactional
    public ImagenProductoStore.ImagenGuardada guardarImagenProducto(Long productoId, String contentType, byte[] bytes, Integer orden) {
        return imagenProductoStore.guardar(productoId, contentType, bytes, orden);
    }

    @Transactional
    public void eliminarImagenProducto(Long productoId, Long imagenId) {
        imagenProductoStore.eliminar(productoId, imagenId);
    }

    @Transactional
    public List<ImagenProductoStore.ImagenGuardada> reordenarImagenesProducto(Long productoId, List<Long> imagenesIds) {
        return imagenProductoStore.reordenar(productoId, imagenesIds);
    }
}
