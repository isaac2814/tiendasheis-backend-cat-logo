package com.sheis.catalogo.service;

import com.sheis.catalogo.domain.*;
import com.sheis.catalogo.exception.BadRequestException;
import com.sheis.catalogo.exception.ResourceConflictException;
import com.sheis.catalogo.exception.ResourceNotFoundException;
import com.sheis.catalogo.repository.*;
import com.sheis.catalogo.stock.StockProvider;
import com.sheis.catalogo.web.dto.admin.*;
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
                                ImagenServicioStore imagenServicioStore) {
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
                    planes
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
                            agotado
                    );
                })
                .toList();

        AdminAjustesDto ajustes = obtenerAjustes();

        return new AdminCatalogoSnapshot(serviciosDtos, combosDtos, descuentos, ajustes);
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
    public ImagenServicioStore.ImagenGuardada guardarImagen(String codigo, byte[] bytes, String declaredContentType) {
        if (!skuCategoriaRepository.existsByCodigo(codigo)) {
            throw new ResourceNotFoundException("El servicio '" + codigo + "' no existe");
        }
        String detectedContentType = ImageValidator.validarYDetectarContentType(bytes, declaredContentType);
        return imagenServicioStore.guardar(codigo, detectedContentType, bytes);
    }

    @Transactional
    public void eliminarImagen(String codigo) {
        imagenServicioStore.eliminar(codigo);
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
        }

        comboRepository.deleteAll();
        comboRepository.flush();

        List<Combo> nuevos = new ArrayList<>();
        int defaultOrden = 1;
        for (AdminComboInputDto in : inputs) {
            List<SkuCategoria> servicios = skuCategoriaRepository.findByCodigoIn(in.servicios());
            if (servicios.size() != in.servicios().size()) {
                throw new BadRequestException("Uno o más códigos de servicio especificados en el combo no existen");
            }

            Combo c = Combo.builder()
                    .etiqueta(in.etiqueta() != null ? in.etiqueta().trim() : "")
                    .activo(in.activo() != null ? in.activo() : true)
                    .orden(in.orden() != null ? in.orden() : defaultOrden++)
                    .servicios(new ArrayList<>(servicios))
                    .build();

            nuevos.add(c);
        }

        comboRepository.saveAll(nuevos);
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

        List<AdminMetodoPagoDto> metodos = metodoPagoRepository.findAllByOrderByOrdenAsc().stream()
                .map(m -> new AdminMetodoPagoDto(
                        m.getId(),
                        m.getNombre(),
                        m.getInicial(),
                        m.getColor(),
                        m.getEtiquetaDato(),
                        m.getDato(),
                        m.getTitular(),
                        m.getActivo(),
                        m.getOrden()
                ))
                .toList();

        return new AdminAjustesDto(whatsapp, sobreNosotros, horarios, metodos);
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

        return obtenerAjustes();
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
                planes
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
}
