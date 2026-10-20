package com.sheis.catalogo.service;

import com.sheis.catalogo.domain.*;
import com.sheis.catalogo.repository.*;
import com.sheis.catalogo.stock.StockProvider;
import com.sheis.catalogo.web.dto.publico.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CatalogoPublicoService {

    private final SkuCategoriaRepository skuCategoriaRepository;
    private final ComboRepository comboRepository;
    private final DescuentoComboRepository descuentoComboRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final HorarioRepository horarioRepository;
    private final CatalogoConfigRepository catalogoConfigRepository;
    private final PricingService pricingService;
    private final StockProvider stockProvider;
    private final ImagenServicioStore imagenServicioStore;
    private final ImagenMetodoPagoStore imagenMetodoPagoStore;

    public CatalogoPublicoService(SkuCategoriaRepository skuCategoriaRepository,
                                  ComboRepository comboRepository,
                                  DescuentoComboRepository descuentoComboRepository,
                                  MetodoPagoRepository metodoPagoRepository,
                                  HorarioRepository horarioRepository,
                                  CatalogoConfigRepository catalogoConfigRepository,
                                  PricingService pricingService,
                                  StockProvider stockProvider,
                                  ImagenServicioStore imagenServicioStore,
                                  ImagenMetodoPagoStore imagenMetodoPagoStore) {
        this.skuCategoriaRepository = skuCategoriaRepository;
        this.comboRepository = comboRepository;
        this.descuentoComboRepository = descuentoComboRepository;
        this.metodoPagoRepository = metodoPagoRepository;
        this.horarioRepository = horarioRepository;
        this.catalogoConfigRepository = catalogoConfigRepository;
        this.pricingService = pricingService;
        this.stockProvider = stockProvider;
        this.imagenServicioStore = imagenServicioStore;
        this.imagenMetodoPagoStore = imagenMetodoPagoStore;
    }

    @Transactional(readOnly = true)
    @Cacheable("catalogo")
    public CatalogoPublicoResponse obtenerCatalogoPublico() {
        // 1. Descuentos por volumen
        List<DescuentoCombo> descList = descuentoComboRepository.findByActivoTrueOrderByCantidadServiciosAsc();
        Map<Integer, Long> descPorCantidad = descList.stream()
                .collect(Collectors.toMap(
                        DescuentoCombo::getCantidadServicios,
                        d -> d.getDescuento().longValue()
                ));

        Map<String, Long> descPorCantidadStr = descList.stream()
                .collect(Collectors.toMap(
                        d -> String.valueOf(d.getCantidadServicios()),
                        d -> d.getDescuento().longValue()
                ));

        long extraDesde6 = catalogoConfigRepository.findByClave("descuento_extra_desde_6")
                .map(c -> {
                    try {
                        return Long.parseLong(c.getValor().trim());
                    } catch (NumberFormatException e) {
                        return 2000L;
                    }
                })
                .orElse(2000L);

        DescuentosDto descuentosDto = new DescuentosDto(descPorCantidadStr, extraDesde6);

        // 2. Servicios visibles con al menos un plan activo
        List<SkuCategoria> visibles = skuCategoriaRepository.findVisibleWithTarifas();
        Map<String, Instant> versionesImagenes = imagenServicioStore.obtenerVersionesPorCodigo();
        Map<String, Long> preciosBaseMap = new HashMap<>();
        Map<String, Boolean> agotadoMap = new HashMap<>();
        List<ServicioPublicoDto> serviciosDtos = new ArrayList<>();

        for (SkuCategoria s : visibles) {
            List<Tarifa> activas = s.getTarifas().stream()
                    .filter(t -> Boolean.TRUE.equals(t.getActivo()))
                    .sorted(Comparator.comparingInt(Tarifa::getDias).thenComparingInt(Tarifa::getCantidadPerfiles))
                    .toList();

            if (activas.isEmpty()) {
                // Sin planes activos, el servicio no se publica
                continue;
            }

            Optional<Long> optBase = pricingService.calcularPrecioBase(activas);
            if (optBase.isEmpty()) {
                continue;
            }
            long base = optBase.get();
            preciosBaseMap.put(s.getCodigo(), base);

            Integer stock = stockProvider.stockDe(s);
            boolean agotado = stock != null && stock == 0;
            agotadoMap.put(s.getCodigo(), agotado);

            List<PlanDto> planesDto = activas.stream()
                    .map(t -> new PlanDto(t.getDias(), t.getCantidadPerfiles(), t.getPrecio().longValue()))
                    .toList();

            Instant imgActualizada = versionesImagenes.get(s.getCodigo());
            String imagenUrl = imgActualizada != null
                    ? "/api/public/servicios/" + s.getCodigo() + "/imagen?v=" + imgActualizada.toEpochMilli()
                    : null;

            serviciosDtos.add(new ServicioPublicoDto(
                    s.getCodigo(),
                    s.getNombre(),
                    s.getInicial(),
                    s.getColor(),
                    s.getLogoUrl(),
                    imagenUrl,
                    s.getEtiqueta() != null ? s.getEtiqueta() : "",
                    s.getDescripcion() != null ? s.getDescripcion() : "",
                    s.getIncluye() != null ? s.getIncluye() : List.of(),
                    s.getRecomendaciones() != null ? s.getRecomendaciones() : List.of(),
                    stock,
                    agotado,
                    base,
                    planesDto,
                    s.getLema(),
                    s.getOrden() != null ? s.getOrden() : 0
            ));
        }

        // 3. Combos activos con todos sus servicios visibles y publicados
        List<Combo> combosActivos = comboRepository.findActiveWithServicios();
        List<ComboPublicoDto> combosDtos = new ArrayList<>();

        for (Combo c : combosActivos) {
            List<SkuCategoria> items = c.getServicios();
            // Validar que todos los servicios estén visibles y tengan precio base publicado
            boolean todosVisibles = items != null && !items.isEmpty() &&
                    items.stream().allMatch(it -> Boolean.TRUE.equals(it.getVisible()) && preciosBaseMap.containsKey(it.getCodigo()));

            if (!todosVisibles) {
                continue;
            }

            List<String> codigos = items.stream().map(SkuCategoria::getCodigo).toList();
            List<Long> preciosBaseCombo = codigos.stream().map(preciosBaseMap::get).toList();

            PricingService.ComboPricing pricing = pricingService.calcularPrecioCombo(preciosBaseCombo, descPorCantidad, extraDesde6);
            boolean comboAgotado = codigos.stream().anyMatch(cod -> Boolean.TRUE.equals(agotadoMap.get(cod)));

            combosDtos.add(new ComboPublicoDto(
                    c.getId(),
                    c.getEtiqueta() != null ? c.getEtiqueta() : "",
                    codigos,
                    pricing.precioNormal(),
                    pricing.descuento(),
                    pricing.precio(),
                    comboAgotado,
                    c.getOrden() != null ? c.getOrden() : 0,
                    c.getVisible() != null ? c.getVisible() : (c.getActivo() != null ? c.getActivo() : true)
            ));
        }

        // 4. Métodos de pago activos
        Map<String, Instant> versionesImagenesMetodos = imagenMetodoPagoStore.obtenerVersionesPorId();
        List<MetodoPagoDto> metodosPagoDtos = metodoPagoRepository.findByActivoTrueOrderByOrdenAsc().stream()
                .map(m -> {
                    Instant imgActualizada = versionesImagenesMetodos.get(m.getId());
                    String imagenUrl = imgActualizada != null
                            ? "/api/public/metodos-pago/" + m.getId() + "/imagen?v=" + imgActualizada.toEpochMilli()
                            : null;
                    return new MetodoPagoDto(
                            m.getId(),
                            m.getNombre(),
                            m.getInicial(),
                            m.getColor(),
                            m.getEtiquetaDato(),
                            m.getDato(),
                            m.getTitular(),
                            imagenUrl
                    );
                })
                .toList();

        // 5. Horario
        List<HorarioDto> horarioDtos = horarioRepository.findAllByOrderByOrdenAsc().stream()
                .map(h -> new HorarioDto(h.getDia(), h.getTexto()))
                .toList();

        // 6. Configuración suelta y textos
        String whatsapp = catalogoConfigRepository.findByClave("whatsapp")
                .map(CatalogoConfig::getValor)
                .orElse("57 300 000 0000");

        String sobreNosotros = catalogoConfigRepository.findByClave("sobre_nosotros")
                .map(CatalogoConfig::getValor)
                .orElse("");

        CatalogoTextosDto textos = obtenerTextosConfig();

        // 7. Lista de favoritos ordenada
        record TempFav(String tipo, String ref, int orden) {}
        List<TempFav> tempFavs = new ArrayList<>();

        Set<String> publishedSvcCodes = serviciosDtos.stream().map(ServicioPublicoDto::id).collect(Collectors.toSet());
        for (SkuCategoria s : visibles) {
            if (s.getFavoritoOrden() != null && publishedSvcCodes.contains(s.getCodigo())) {
                tempFavs.add(new TempFav("servicio", s.getCodigo(), s.getFavoritoOrden()));
            }
        }

        Set<Long> publishedComboIds = combosDtos.stream()
                .filter(ComboPublicoDto::visible)
                .map(ComboPublicoDto::id)
                .collect(Collectors.toSet());
        for (Combo c : combosActivos) {
            boolean visible = c.getVisible() != null ? c.getVisible() : (c.getActivo() != null ? c.getActivo() : true);
            if (visible && c.getFavoritoOrden() != null && publishedComboIds.contains(c.getId())) {
                tempFavs.add(new TempFav("combo", String.valueOf(c.getId()), c.getFavoritoOrden()));
            }
        }

        tempFavs.sort(Comparator.comparingInt(TempFav::orden));
        List<FavoritoDto> favoritosDtos = tempFavs.stream()
                .map(f -> new FavoritoDto(f.tipo(), f.ref()))
                .toList();

        return new CatalogoPublicoResponse(
                serviciosDtos,
                combosDtos,
                descuentosDto,
                metodosPagoDtos,
                horarioDtos,
                whatsapp,
                sobreNosotros,
                textos,
                favoritosDtos
        );
    }

    private String obtenerConfig(String snakeClave, String camelClave, String def, boolean permitirVacio) {
        return catalogoConfigRepository.findByClave(snakeClave)
                .or(() -> catalogoConfigRepository.findByClave(camelClave))
                .map(CatalogoConfig::getValor)
                .map(String::trim)
                .filter(v -> permitirVacio || !v.isEmpty())
                .orElse(def);
    }

    public CatalogoTextosDto obtenerTextosConfig() {
        return new CatalogoTextosDto(
                obtenerConfig("banner_texto", "bannerTexto", "Elige tu mejor servicio y empieza tu maratón", true),
                obtenerConfig("titulo_favoritos", "tituloFavoritos", "Servicios favoritos", false),
                obtenerConfig("titulo_servicios", "tituloServicios", "Servicios", false),
                obtenerConfig("titulo_combos", "tituloCombos", "Combos", false),
                obtenerConfig("menu_favoritos", "menuFavoritos", "Servicios favoritos", false),
                obtenerConfig("menu_servicios", "menuServicios", "Servicios individuales", false),
                obtenerConfig("menu_combos", "menuCombos", "Combos con descuento", false)
        );
    }

    @Transactional(readOnly = true)
    public Optional<ImagenServicioStore.ImagenData> obtenerImagen(String codigo) {
        return imagenServicioStore.obtener(codigo);
    }

    @Transactional(readOnly = true)
    public Optional<ImagenMetodoPagoStore.ImagenData> obtenerImagenMetodoPago(String id) {
        return imagenMetodoPagoStore.obtener(id);
    }
}
