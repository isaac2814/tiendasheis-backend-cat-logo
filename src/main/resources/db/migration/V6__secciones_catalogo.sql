-- ============================================================================
-- V6: Secciones del catálogo, textos personalizables, lema y lista de favoritos
-- ============================================================================
-- Migración aditiva y segura para producción (Supabase / PostgreSQL y H2).
-- No borra ni sobreescribe datos que el administrador ya haya editado.
-- ============================================================================

-- 1) Nuevas columnas en sku_categorias (servicios)
ALTER TABLE sku_categorias ADD COLUMN IF NOT EXISTS lema VARCHAR(30);
ALTER TABLE sku_categorias ADD COLUMN IF NOT EXISTS favorito_orden INT;

-- 2) Nuevas columnas en combos
ALTER TABLE combos ADD COLUMN IF NOT EXISTS visible BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE combos ADD COLUMN IF NOT EXISTS favorito_orden INT;

-- Sincronizar visible con activo para filas existentes si aplica
UPDATE combos SET visible = activo WHERE visible IS NULL;

-- 3) Textos configurables del catálogo en catalogo_config (idempotente)
INSERT INTO catalogo_config (clave, valor) VALUES ('banner_texto', 'Elige tu mejor servicio y empieza tu maratón') ON CONFLICT (clave) DO NOTHING;
INSERT INTO catalogo_config (clave, valor) VALUES ('titulo_favoritos', 'Servicios favoritos') ON CONFLICT (clave) DO NOTHING;
INSERT INTO catalogo_config (clave, valor) VALUES ('titulo_servicios', 'Servicios') ON CONFLICT (clave) DO NOTHING;
INSERT INTO catalogo_config (clave, valor) VALUES ('titulo_combos', 'Combos') ON CONFLICT (clave) DO NOTHING;
INSERT INTO catalogo_config (clave, valor) VALUES ('menu_favoritos', 'Servicios favoritos') ON CONFLICT (clave) DO NOTHING;
INSERT INTO catalogo_config (clave, valor) VALUES ('menu_servicios', 'Servicios individuales') ON CONFLICT (clave) DO NOTHING;
INSERT INTO catalogo_config (clave, valor) VALUES ('menu_combos', 'Combos con descuento') ON CONFLICT (clave) DO NOTHING;

-- 4) Lemas iniciales por código de servicio (solo si lema es NULL)
UPDATE sku_categorias SET lema = 'El más codiciado' WHERE codigo = 'nc' AND lema IS NULL;
UPDATE sku_categorias SET lema = 'El más exclusivo' WHERE codigo = 'ne' AND lema IS NULL;
UPDATE sku_categorias SET lema = 'El más familiar' WHERE codigo = 'de' AND lema IS NULL;
UPDATE sku_categorias SET lema = 'Calidad de cine' WHERE codigo = 'dp' AND lema IS NULL;
UPDATE sku_categorias SET lema = 'Series de culto' WHERE codigo = 'hb' AND lema IS NULL;
UPDATE sku_categorias SET lema = 'El más completo' WHERE codigo = 'pr' AND lema IS NULL;
UPDATE sku_categorias SET lema = 'El más otaku' WHERE codigo = 'cr' AND lema IS NULL;
UPDATE sku_categorias SET lema = 'El más taquillero' WHERE codigo = 'pa' AND lema IS NULL;
UPDATE sku_categorias SET lema = 'El más novelero' WHERE codigo = 'vx' AND lema IS NULL;
UPDATE sku_categorias SET lema = 'El más creativo' WHERE codigo = 'cv' AND lema IS NULL;
UPDATE sku_categorias SET lema = 'El más melómano' WHERE codigo = 'sp' AND lema IS NULL;

-- 5) Favoritos iniciales:
-- Solo se asignan si no hay ningún favorito previamente configurado.
-- (1) Netflix Compartido (nc)
UPDATE sku_categorias
SET favorito_orden = 1
WHERE codigo = 'nc'
  AND favorito_orden IS NULL
  AND NOT EXISTS (SELECT 1 FROM sku_categorias WHERE favorito_orden IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM combos WHERE favorito_orden IS NOT NULL);

-- (2) Combo de exactamente Netflix Compartido + Disney Estándar (nc + de)
UPDATE combos
SET favorito_orden = 2,
    etiqueta = 'El combo favorito'
WHERE id IN (
    SELECT ci.combo_id
    FROM combo_items ci
    JOIN sku_categorias s ON ci.sku_categoria_id = s.id
    GROUP BY ci.combo_id
    HAVING COUNT(*) = 2
       AND COUNT(CASE WHEN s.codigo IN ('nc', 'de') THEN 1 END) = 2
)
AND favorito_orden IS NULL
AND (SELECT COUNT(*) FROM sku_categorias WHERE favorito_orden IS NOT NULL) = 1
AND NOT EXISTS (SELECT 1 FROM combos WHERE favorito_orden IS NOT NULL);

-- (3) Combos de exactamente 3 servicios, en su orden actual
UPDATE combos
SET favorito_orden = (
    SELECT 2 + COUNT(*)
    FROM combos c2
    WHERE (c2.orden < combos.orden OR (c2.orden = combos.orden AND c2.id <= combos.id))
      AND c2.id IN (
          SELECT combo_id
          FROM combo_items
          GROUP BY combo_id
          HAVING COUNT(*) = 3
      )
)
WHERE id IN (
    SELECT combo_id
    FROM combo_items
    GROUP BY combo_id
    HAVING COUNT(*) = 3
)
AND favorito_orden IS NULL
AND (SELECT COUNT(*) FROM combos WHERE favorito_orden IS NOT NULL) = 1;
