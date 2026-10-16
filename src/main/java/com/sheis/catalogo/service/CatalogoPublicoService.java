package com.sheis.catalogo.service;

import com.sheis.catalogo.domain.*;
import com.sheis.catalogo.repository.*;
import com.sheis.catalogo.stock.StockProvider;
import com.sheis.catalogo.web.dto.publico.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public CatalogoPublicoService(SkuCategoriaRepository skuCategoriaRepository,
                                  ComboRepository comboRepository,
                                  DescuentoComboRepository descuentoComboRepository,
                                  MetodoPagoRepository metodoPagoRepository,
                                  HorarioRepository horarioRepository,
                                  CatalogoConfigRepository catalogoConfigRepository,
                                  PricingService pricingService,
                                  StockProvider stockProvider) {
        this.skuCategoriaRepository = skuCategoriaRepository;
        this.comboRepository = comboRepository;
        this.descuentoComboRepository = descuentoComboRepository;
        this.metodoPagoRepository = metodoPagoRepository;
        this.horarioRepository = horarioRepository;
        this.catalogoConfigRepository = catalogoConfigRepository;
        this.pricingService = pricingService;
        this.stockProvider = stockProvider;
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

            serviciosDtos.add(new ServicioPublicoDto(
                    s.getCodigo(),
                    s.getNombre(),
                    s.getInicial(),
                    s.getColor(),
                    s.getLogoUrl(),
                    s.getEtiqueta() != null ? s.getEtiqueta() : "",
                    s.getDescripcion() != null ? s.getDescripcion() : "",
                    s.getIncluye() != null ? s.getIncluye() : List.of(),
                    s.getRecomendaciones() != null ? s.getRecomendaciones() : List.of(),
                    stock,
                    agotado,
                    base,
                    planesDto
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
                    comboAgotado
            ));
        }

        // 4. Métodos de pago activos
        List<MetodoPagoDto> metodosPagoDtos = metodoPagoRepository.findByActivoTrueOrderByOrdenAsc().stream()
                .map(m -> new MetodoPagoDto(
                        m.getId(),
                        m.getNombre(),
                        m.getInicial(),
                        m.getColor(),
                        m.getEtiquetaDato(),
                        m.getDato(),
                        m.getTitular()
                ))
                .toList();

        // 5. Horario
        List<HorarioDto> horarioDtos = horarioRepository.findAllByOrderByOrdenAsc().stream()
                .map(h -> new HorarioDto(h.getDia(), h.getTexto()))
                .toList();

        // 6. Configuración suelta
        String whatsapp = catalogoConfigRepository.findByClave("whatsapp")
                .map(CatalogoConfig::getValor)
                .orElse("57 300 000 0000");

        String sobreNosotros = catalogoConfigRepository.findByClave("sobre_nosotros")
                .map(CatalogoConfig::getValor)
                .orElse("");

        return new CatalogoPublicoResponse(
                serviciosDtos,
                combosDtos,
                descuentosDto,
                metodosPagoDtos,
                horarioDtos,
                whatsapp,
                sobreNosotros
        );
    }
}
