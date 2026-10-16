package com.sheis.catalogo.service;

import com.sheis.catalogo.domain.Tarifa;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PricingService {

    public record ComboPricing(long precioNormal, long descuento, long precio) {}

    /**
     * Calcula el precio base de un servicio a partir de sus planes activos:
     * 1. Plan activo con dias=30 y cantidad_perfiles=1.
     * 2. Si no existe, el primer plan activo ordenado por menor días, luego menor perfiles.
     * 3. Si no hay planes activos, retorna Optional.empty() (el servicio no se publica).
     */
    public Optional<Long> calcularPrecioBase(List<Tarifa> tarifas) {
        if (tarifas == null || tarifas.isEmpty()) {
            return Optional.empty();
        }

        List<Tarifa> activas = tarifas.stream()
                .filter(t -> Boolean.TRUE.equals(t.getActivo()))
                .filter(t -> t.getPrecio() != null)
                .toList();

        if (activas.isEmpty()) {
            return Optional.empty();
        }

        // Buscar primero plan 30 días, 1 perfil
        Optional<Tarifa> plan30 = activas.stream()
                .filter(t -> t.getDias() == 30 && t.getCantidadPerfiles() == 1)
                .findFirst();

        if (plan30.isPresent()) {
            return Optional.of(plan30.get().getPrecio().longValue());
        }

        // Fallback: menor días, luego menor perfiles
        return activas.stream()
                .min(Comparator.comparingInt(Tarifa::getDias)
                        .thenComparingInt(Tarifa::getCantidadPerfiles))
                .map(t -> t.getPrecio().longValue());
    }

    /**
     * Calcula el descuento para n servicios distintos:
     * - n < 2 -> 0
     * - 2 <= n <= 5 -> valor activo de descuentos_combo (si falta, 0)
     * - n > 5 -> descuento(5) + (n - 5) * descuento_extra_desde_6
     */
    public long calcularDescuento(int n, Map<Integer, Long> descuentosPorCantidad, long descuentoExtraDesde6) {
        if (n < 2) {
            return 0L;
        }

        if (n <= 5) {
            return descuentosPorCantidad != null ? descuentosPorCantidad.getOrDefault(n, 0L) : 0L;
        }

        long descuento5 = descuentosPorCantidad != null ? descuentosPorCantidad.getOrDefault(5, 0L) : 0L;
        return descuento5 + ((long) (n - 5) * descuentoExtraDesde6);
    }

    /**
     * Calcula los valores de precio de un combo a partir de los precios base de sus servicios:
     * - precioNormal = suma de precios base
     * - descuento = descuento(n)
     * - precio = precioNormal - descuento
     */
    public ComboPricing calcularPrecioCombo(List<Long> preciosBase, Map<Integer, Long> descuentosPorCantidad, long descuentoExtraDesde6) {
        if (preciosBase == null || preciosBase.isEmpty()) {
            return new ComboPricing(0L, 0L, 0L);
        }

        long precioNormal = preciosBase.stream().mapToLong(Long::longValue).sum();
        long descuento = calcularDescuento(preciosBase.size(), descuentosPorCantidad, descuentoExtraDesde6);
        long precio = precioNormal - descuento;

        return new ComboPricing(precioNormal, descuento, precio);
    }
}
