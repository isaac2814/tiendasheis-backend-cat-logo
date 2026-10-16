package com.sheis.catalogo.stock;

import com.sheis.catalogo.domain.SkuCategoria;
import org.springframework.stereotype.Component;

@Component
public class ManualStockProvider implements StockProvider {

    @Override
    public Integer stockDe(SkuCategoria c) {
        return c != null ? c.getStockManual() : null;
    }
}
