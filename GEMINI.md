# Backend Catálogo TiendaSheis — Reglas de Lógica de Negocio y Arquitectura

## 1. Jerarquía del Modelo de Negocio
Todo el sistema debe respetar estrictamente esta jerarquía de 3 niveles:
1. **Servicio**: Plataforma base (ej. Netflix, Disney+, Spotify, Max, Prime Video).
2. **Modalidad**: Tipo o variante de cuenta dentro del servicio (SkuCategoria):
   - **Netflix**: `Compartido` y `Exclusivo`.
   - **Disney+**: `Estándar` y `Premium`.
   - **Spotify**: `Familiar` e `Individual`.
   - **Max**: `Estándar` y `Platino`.
   - **Otros servicios**: `Estándar`, `Premium`, `Compartido`, `Exclusivo`, o personalizada.
3. **Tarifas / Planes**: Configuración de precio de una modalidad específica:
   - **Cantidad de perfiles**: 1 perfil, 2 perfiles, 3 perfiles... o Cuenta Completa.
   - **Tiempo de servicio**: 15 días, 1 mes (30 días), 2 meses (60 días), etc.
   - **Precio Final (\$)** (PVP) y **Precio Distribuidor (\$)**.

> [!IMPORTANT]
> Modificar la cantidad de perfiles o el tiempo de servicio define una tarifa/precio independiente **sin alterar ni sobrescribir la modalidad**.

---

## 2. Precios de Paquete Cerrados
- El precio de una tarifa es un **valor fijo del paquete**.
- **NUNCA** se calcula multiplicando `precio_unitario × cantidad_perfiles`.
- Ejemplo: 1 perfil = \$14.000, 2 perfiles = \$26.000 (no \$28.000).
- El motor de precios (`PricingService`) aplica descuentos progresivos por combo sobre los precios cerrados de cada tarifa.

---

## 3. Motor de Precios y Descuentos
- **Precio base**: Valor fijo definido en la tarifa (`precio_pvp` o `precio_distribuidor`).
- **Descuentos por combo**: Escalones progresivos definidos en `DescuentoCombo`.
  - Ejemplo: 2 servicios = 5% dto, 3 servicios = 10% dto, 4+ servicios = 15% dto.
- **Cálculo**: `PricingService` suma precios base y aplica el descuento correspondiente al escalón alcanzado.
- **Combos**: Agrupaciones predefinidas de servicios con precio especial en `Combo`.

---

## 4. Inventario de Stock
- Stock gestionado mediante la interfaz `StockProvider`.
- Implementación actual: `ManualStockProvider` — lee `stock_manual` del servicio.
- Stock > 0 → "Disponible"; Stock = 0 → "Agotado".
- Diseñado para futuras implementaciones automatizadas sin modificar servicios existentes.

---

## 5. Visibilidad del Catálogo
- Servicios con `visible = false` **no aparecen** en el endpoint público `GET /api/catalogo`.
- El administrador puede alternar visibilidad vía `PATCH /api/admin/servicios/{codigo}/visible`.
- Un servicio oculto sigue existiendo en la base de datos y en la vista admin.

---

## 6. Configuración Global del Catálogo
- `CatalogoConfig` almacena: nombre del negocio, número de WhatsApp, nota general, moneda.
- `Horario` almacena horarios de atención por día.
- `MetodoPago` almacena los métodos de pago aceptados con su información.
- Todo configurable desde `PUT /api/admin/ajustes`.

---

## 7. Autenticación y Autorización
- **Login**: `POST /api/auth/login` → JWT HS256 (configurable TTL).
- **Protección**: Todos los endpoints `/api/admin/**` requieren JWT con rol `ADMIN`.
- **Rate limiting**: 5 intentos fallidos por IP+email en 15 minutos → 429.
- **Preparado para migrar** a Supabase Auth (ES256/JWKS) sin cambios en la capa de negocio.

---

## 8. Reglas de Integridad
- **Código de servicio**: Único e inmutable (slug URL-friendly, ej. `netflix`, `disney-plus`).
- **Combinación única de tarifa**: `(sku_categoria_id, dias, perfiles)` no puede repetirse dentro del mismo servicio.
- **Precios**: Siempre `BigDecimal`, nunca `float` ni `double`.
- **UUIDs**: Todas las claves primarias son UUID generados por la base de datos.
