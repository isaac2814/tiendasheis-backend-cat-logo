-- ============================================================================
-- V8: Índices aditivos para optimizar consultas de productos de tecnología
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_producto_categoria_visible ON producto(categoria_id, visible);
CREATE INDEX IF NOT EXISTS idx_producto_variante_prod_activa ON producto_variante(producto_id, activa);
