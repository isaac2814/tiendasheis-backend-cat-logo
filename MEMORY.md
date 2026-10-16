# MEMORY.md — Memoria Persistente del Backend Catálogo

> [!CAUTION]
> **🚨 MANDATO INELUDIBLE PARA AGENTES**
> 1. **Al iniciar**: Leer este archivo completo antes de tocar cualquier código.
> 2. **Al finalizar**: Actualizar este archivo automáticamente con decisiones, cambios y estado resultante. **Ninguna tarea se considera terminada sin este paso.**

> [!IMPORTANT]
> **`AGENTS.md` tiene prioridad absoluta** sobre cualquier contenido de este archivo. En caso de conflicto entre ambos: `AGENTS.md` gana siempre.

> [!WARNING]
> **Nunca incluir en este archivo:** contraseñas, API Keys, tokens, JWT secrets, credenciales de base de datos, datos personales de clientes, variables `.env` o cualquier información sensible.

---

## 1. Identidad del Proyecto

- **Nombre**: Backend Catálogo — TiendaSheis
- **Tipo**: API REST y motor de precios para el catálogo público de streaming de TiendaSheis
- **Repositorio**: `C:\Sheis catálogo\Backend catalogo`
- **Frontend asociado**: `C:\Sheis catálogo\front` (NO TOCAR desde este proyecto)
- **Puerto por defecto**: `8080`
- **Versión de migración Flyway activa**: `V2`
- **Próxima migración**: `V3`

---

## 2. Stack Tecnológico

| Componente | Tecnología |
|:--|:--|
| Runtime | Java 25 (OpenJDK) |
| Framework | Spring Boot 4.1.1 |
| Autenticación | JWT HS256 local (OAuth2 Resource Server) |
| ORM | Hibernate ORM 7 + Spring Data JPA |
| Base de Datos | PostgreSQL 15+ (producción) / H2 en memoria (tests) |
| Migraciones | Flyway (última: V2) |
| Build | Apache Maven 3.9+ (wrapper incluido) |
| Boilerplate | Project Lombok (`@Getter`, `@Setter`, `@Slf4j` — constructores manuales por Java 25) |
| Caché | Caffeine (10 min TTL, máx 100 entradas) |
| Documentación API | Springdoc OpenAPI 3.1.1 / Swagger UI |
| Seguridad de contraseñas | BCrypt |
| Rate Limiting | Caffeine (login: 5 intentos / 15 min por IP+email) |

---

## 3. Arquitectura

### Capas del backend
```
Controller (@RestController, /api/{recurso})
    ↓
Service (lógica de negocio, cálculos de precio, caché)
    ↓
Repository (JpaRepository<Entidad, UUID>)
    ↓
PostgreSQL / H2
```

### Autenticación y autorización
- Backend = OAuth2 Resource Server: emite y valida JWT HS256 localmente
- `TokenService` genera tokens; `NimbusJwtEncoder`/`NimbusJwtDecoder` firma y verifica
- `AuthService` valida credenciales contra `AdminUsuario` con BCrypt
- `LoginRateLimiter` protege contra fuerza bruta (Caffeine cache)
- Autorización: `@PreAuthorize("hasRole('ADMIN')")` en rutas `/api/admin/**`
- Preparado para migración futura a Supabase Auth (solo cambiar `JwtDecoder` a JWKS)

### Stock desacoplado
- Interfaz `StockProvider` → implementación `ManualStockProvider` (lee `stock_manual`)
- Extensible: crear nueva implementación con `@Primary` sin modificar servicios

---

## 4. Estructura del Proyecto

```
C:\Sheis catálogo\Backend catalogo\
├── AGENTS.md                  # Constitución del agente (reglas absolutas)
├── MEMORY.md                  # Este archivo (memoria viva)
├── GEMINI.md                  # Reglas de negocio resumidas
├── README.md                  # Guía de setup y ejecución
│
├── docs/
│   └── api.md                 # Documentación completa de la API REST
│
├── pom.xml
├── mvnw / mvnw.cmd
├── .env.example
├── .gitignore
│
└── src/
    ├── main/java/com/sheis/catalogo/
    │   ├── CatalogoApplication.java
    │   ├── config/
    │   │   ├── AppProperties.java      # Propiedades app.cors, app.jwt, app.admin
    │   │   ├── DataInitializer.java     # Crea admin inicial si tabla vacía
    │   │   └── OpenApiConfig.java       # Configuración Swagger/OpenAPI
    │   ├── domain/
    │   │   ├── AdminUsuario.java        # Usuarios administradores
    │   │   ├── CatalogoConfig.java      # Configuración global del catálogo
    │   │   ├── Combo.java               # Combos de servicios
    │   │   ├── DescuentoCombo.java      # Escalones de descuento por combo
    │   │   ├── Horario.java             # Horarios de atención
    │   │   ├── MetodoPago.java          # Métodos de pago aceptados
    │   │   ├── Servicio.java            # Servicios de streaming
    │   │   ├── SkuCategoria.java        # Modalidades / SKUs por servicio
    │   │   └── Tarifa.java              # Tarifas (días + perfiles → precio)
    │   ├── exception/
    │   │   ├── BadRequestException.java
    │   │   ├── GlobalExceptionHandler.java  # @RestControllerAdvice — RFC 9457
    │   │   ├── RateLimitException.java
    │   │   ├── ResourceConflictException.java
    │   │   └── ResourceNotFoundException.java
    │   ├── repository/
    │   │   ├── AdminUsuarioRepository.java
    │   │   ├── CatalogoConfigRepository.java
    │   │   ├── ComboRepository.java
    │   │   ├── DescuentoComboRepository.java
    │   │   ├── HorarioRepository.java
    │   │   ├── MetodoPagoRepository.java
    │   │   ├── ServicioRepository.java
    │   │   ├── SkuCategoriaRepository.java
    │   │   └── TarifaRepository.java
    │   ├── security/
    │   │   ├── AuthService.java          # Autenticación BCrypt
    │   │   ├── LoginRateLimiter.java     # Rate limiting por IP+email
    │   │   ├── SecurityConfig.java       # OAuth2 Resource Server + CORS
    │   │   └── TokenService.java         # Generación/validación JWT HS256
    │   ├── service/
    │   │   ├── CatalogoAdminService.java  # CRUD admin del catálogo
    │   │   ├── CatalogoPublicoService.java # Lectura pública + caché
    │   │   └── PricingService.java        # Motor de cálculo de precios
    │   ├── stock/
    │   │   ├── StockProvider.java         # Interfaz de stock
    │   │   └── ManualStockProvider.java   # Implementación manual
    │   └── web/
    │       ├── AuthController.java        # POST /api/auth/login
    │       ├── CatalogoAdminController.java  # CRUD /api/admin/**
    │       ├── CatalogoPublicoController.java # GET /api/catalogo
    │       └── dto/
    │           ├── admin/                 # DTOs de administración
    │           ├── auth/                  # LoginRequest, LoginResponse
    │           └── publico/               # DTOs del catálogo público
    │
    ├── main/resources/
    │   ├── application.yml
    │   └── db/migration/
    │       ├── V1__catalogo_inicial.sql   # Esquema + datos iniciales
    │       └── V2__admin_usuarios.sql     # Tabla admin_usuarios
    │
    └── test/java/com/sheis/catalogo/
        ├── CatalogoApplicationTests.java
        ├── security/SecurityAndAdminTest.java
        ├── service/PricingServiceTest.java
        └── web/CatalogoPublicoControllerTest.java
```

---

## 5. Base de Datos

### Conexión
- **Producción**: PostgreSQL 15+ (configurable vía `.env`)
- **Tests**: H2 en memoria (`MODE=PostgreSQL`)
- **DDL**: `validate` (Flyway maneja el esquema)

### Registro de migraciones Flyway

| Versión | Descripción | Estado |
|:--|:--|:--|
| V1 | `catalogo_inicial` — esquema completo + datos seed | ✅ Aplicada |
| V2 | `admin_usuarios` — tabla de administradores | ✅ Aplicada |

### Entidades y relaciones

```
Servicio (Netflix, Disney+, Spotify, Max, Prime Video...)
    ↓ 1:N
SkuCategoria / Modalidad (Compartido, Exclusivo, Estándar, Premium...)
    ↓ 1:N
Tarifa (sku_categoria_id + dias + perfiles → precio_pvp, precio_distribuidor)
```

### Entidades clave

| Entidad | PK | Propósito |
|:--|:--|:--|
| `Servicio` | UUID | Plataformas de streaming |
| `SkuCategoria` | UUID | Modalidades/variantes por servicio |
| `Tarifa` | UUID | Paquetes de precio (días + perfiles) |
| `Combo` | UUID | Combos de servicios con descuento |
| `DescuentoCombo` | UUID | Escalones de descuento por cantidad |
| `MetodoPago` | UUID | Métodos de pago aceptados |
| `Horario` | UUID | Horarios de atención |
| `CatalogoConfig` | UUID | Configuración global (WhatsApp, nombre, etc.) |
| `AdminUsuario` | UUID | Usuarios administradores |

---

## 6. Reglas de Negocio

### Jerarquía de negocio — INVARIANTE
```
Servicio → Modalidad (SkuCategoria) → Tarifa (paquete cerrado)
```

### Precios
- **El precio NO se multiplica por perfiles**: es un valor cerrado del paquete
  - Ej: 1 perfil = $14.000, 2 perfiles = $26.000 (no $28.000)
- Motor de precios (`PricingService`): calcula precio base, descuentos progresivos por combo y total

### Duraciones
- Días exactos (no forzar a 30 días)
- Ejemplos: 31 para enero/marzo/mayo; 28 para febrero; 30 para mes comercial

### Stock
- `ManualStockProvider` lee `stock_manual` del servicio
- Stock > 0 → "Disponible"; Stock = 0 → "Agotado"
- Desacoplado para futura automatización

### Visibilidad del catálogo
- Servicios con `visible = false` NO aparecen en el catálogo público
- Admin puede alternar visibilidad sin eliminar

---

## 7. Endpoints Principales

### Públicos (sin autenticación)
| Método | Ruta | Propósito |
|:--|:--|:--|
| `GET` | `/api/catalogo` | Catálogo completo público |

### Autenticación
| Método | Ruta | Propósito |
|:--|:--|:--|
| `POST` | `/api/auth/login` | Login → JWT |

### Admin (requieren JWT con rol ADMIN)
| Método | Ruta | Propósito |
|:--|:--|:--|
| `GET` | `/api/admin/catalogo` | Snapshot completo admin |
| `POST` | `/api/admin/servicios` | Crear servicio |
| `PUT` | `/api/admin/servicios/{codigo}` | Actualizar servicio |
| `DELETE` | `/api/admin/servicios/{codigo}` | Eliminar servicio |
| `PATCH` | `/api/admin/servicios/{codigo}/visible` | Alternar visibilidad |
| `PUT` | `/api/admin/servicios/{codigo}/stock` | Cambiar stock |
| `POST` | `/api/admin/servicios/{codigo}/planes` | Crear plan/tarifa |
| `DELETE` | `/api/admin/servicios/{codigo}/planes/{planId}` | Eliminar plan |
| `PUT` | `/api/admin/combos` | Guardar combos |
| `PUT` | `/api/admin/ajustes` | Guardar ajustes globales |

---

## 8. Pruebas Automatizadas

### Suite de tests (19/19 ✅)

| Clase | Tests | Cobertura |
|:--|:--|:--|
| `PricingServiceTest` | Unitarios | Cálculo de precios, descuentos progresivos, combos |
| `CatalogoPublicoControllerTest` | Contrato API | Formato JSON público, estructura de respuesta |
| `SecurityAndAdminTest` | Integración | Auth 401/403/200, login válido/inválido, rate limiting |
| `CatalogoApplicationTests` | Contexto | Carga del contexto Spring |

**Resultado**: `Tests run: 19, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS`

---

## 9. Decisiones Técnicas (ADRs)

### DEC-001 — H2 para tests, PostgreSQL para producción
**Fecha:** 2026-10-15
**Decisión:** Usar H2 en memoria con `MODE=PostgreSQL` para tests en lugar de Testcontainers o Docker.
**Motivo:** Facilitar ejecución de tests en cualquier entorno sin dependencias externas.
**Impacto:** Entidades JPA diseñadas para compatibilidad dual H2/PostgreSQL.

### DEC-002 — Constructores manuales por incompatibilidad Lombok + Java 25
**Fecha:** 2026-10-15
**Decisión:** Reemplazar `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor` de Lombok con constructores explícitos escritos a mano.
**Motivo:** Lombok genera bytecode incompatible con Java 25 (`VerifyError` en constructores).
**Impacto:** Todas las entidades de dominio, servicios y controladores. Solo `@Getter`, `@Setter` y `@Slf4j` de Lombok son seguros.

### DEC-003 — JWT HS256 local preparado para migrar a Supabase Auth
**Fecha:** 2026-10-15
**Decisión:** Autenticación local con `TokenService` (HS256) usando estándar OAuth2 Resource Server.
**Motivo:** Autonomía total sin dependencia de servicios externos en desarrollo. Migración a Supabase Auth requiere solo cambiar `JwtDecoder` → JWKS.
**Impacto:** `SecurityConfig`, `TokenService`, `AuthService`, `AuthController`.

### DEC-004 — StockProvider desacoplado
**Fecha:** 2026-10-15
**Decisión:** Interfaz `StockProvider` con implementación `ManualStockProvider` por defecto.
**Motivo:** Preparar para futura automatización de stock sin modificar servicios existentes.
**Impacto:** `CatalogoPublicoService`, `CatalogoAdminService`.

### DEC-005 — Precios de paquete cerrados (sin multiplicación)
**Fecha:** 2026-10-15
**Decisión:** El precio de una tarifa es un valor fijo del paquete. Nunca multiplicar por `cantidadPerfiles`.
**Motivo:** 2 perfiles puede tener descuento respecto a 2× precio unitario.
**Impacto:** `PricingService`, DTOs de tarifas, catálogo público.

---

## 10. Problemas Conocidos y Trampas

1. ⚠️ **Lombok + Java 25**: `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor` causan `VerifyError`. Solo usar `@Getter`, `@Setter`, `@Slf4j`.

2. ⚠️ **H2 no soporta `text[]`**: Removido `columnDefinition = "text[]"` de `SkuCategoria`. `@JdbcTypeCode(SqlTypes.ARRAY)` es suficiente.

3. ⚠️ **Spring Boot 4.x — `AutoConfigureMockMvc`**: No está en el paquete esperado. Usar `MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build()` manualmente.

4. ⚠️ **SSL Maven Central**: Errores PKIX al descargar dependencias. Dependencias actuales cacheadas en `.m2` funcionan correctamente.

5. ⚠️ **`BigDecimal` obligatorio**: Nunca usar `float` ni `double` para valores monetarios.

---

## 11. Funcionalidades Completadas

| Funcionalidad | Fecha | Notas |
|:--|:--|:--|
| API REST pública del catálogo (`GET /api/catalogo`) | 2026-10-15 | Catálogo completo con servicios, planes, combos, descuentos, métodos de pago y horarios |
| CRUD admin completo de servicios y planes | 2026-10-15 | Crear, editar, eliminar servicios y tarifas con validaciones |
| Gestión de combos y descuentos progresivos | 2026-10-15 | Escalones de descuento por cantidad de servicios |
| Motor de precios (`PricingService`) | 2026-10-15 | Precio base, descuentos por combo, total calculado |
| Autenticación JWT HS256 con rate limiting | 2026-10-15 | Login, generación de token, protección de rutas admin |
| Administración de ajustes globales del catálogo | 2026-10-15 | WhatsApp, nombre de negocio, nota, horarios, métodos de pago |
| Caché Caffeine para catálogo público | 2026-10-15 | 10 min TTL, evicción automática en cambios admin |
| Swagger UI / OpenAPI interactivo | 2026-10-15 | Documentación y pruebas desde navegador |
| Suite completa de tests (19/19) | 2026-10-15 | Unitarios + integración + contexto |
| DataInitializer para admin inicial | 2026-10-15 | Creación automática del primer administrador |

---

## 12. Funcionalidades Pendientes

- [ ] **Migración a Supabase Auth**: Cambiar `JwtDecoder` de HS256 local a JWKS público de Supabase (ES256).
- [ ] **Stock automatizado**: Implementar `StockProvider` que lea inventario real de cuentas/perfiles.
- [ ] **Imágenes de servicios**: Subida y almacenamiento de logos/banners por servicio.
- [ ] **Auditoría**: Registro de cambios realizados por administradores.
- [ ] **Multi-tenancy**: Soporte para múltiples catálogos/negocios.

---

## 13. Roadmap

*(A definir con el usuario)*

---

## Historial Reciente de Cambios

### 2026-10-15
- **Proyecto creado desde cero**: Backend completo con Spring Boot 4.1.1, Java 25, PostgreSQL + Flyway, H2 para tests.
- **Arquitectura**: Controller → Service → Repository → DB. OAuth2 Resource Server con JWT HS256.
- **19 tests exitosos**: PricingService, CatalogoPublico, Security, contexto Spring.
- **Documentación**: README.md, docs/api.md, .env.example.
- **Resolución**: Incompatibilidad Lombok + Java 25 (constructores manuales).
- **Resolución**: Incompatibilidad H2 + `columnDefinition = "text[]"` (removido).
- **Resolución**: Spring Boot 4.x `AutoConfigureMockMvc` (MockMvc manual).
