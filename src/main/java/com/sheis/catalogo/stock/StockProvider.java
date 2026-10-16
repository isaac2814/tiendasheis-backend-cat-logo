package com.sheis.catalogo.stock;

import com.sheis.catalogo.domain.SkuCategoria;

public interface StockProvider {
    /**
     * Retorna el stock de una categoría de servicio:
     * - null: disponible sin límite
     * - 0: agotado
     * - N: quedan N unidades
     */
    Integer stockDe(SkuCategoria c);
}
