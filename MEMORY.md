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
- **Versión de migración Flyway activa**: `V5`
- **Próxima migración**: `V6`

---

## 2. Stack Tecnológico

| Componente | Tecnología |
|:--|:--|
| Runtime | Java 25 (OpenJDK) |
| Framework | Spring Boot 4.1.1 |
| Autenticación | JWT HS256 local (OAuth2 Resource Server) |
| ORM | Hibernate ORM 7 + Spring Data JPA |
| Base de Datos | PostgreSQL 15+ (producción) / H2 en memoria (tests) |
| Migraciones | Flyway (última: V5) |
| Build | Apache Maven 3.9+ (wrapper incluido) |
| Boilerplate | Project Lombok (`@Getter`, `@Setter`, `@Slf4j` — constructores manuales por Java 25) |
| Caché | Caffeine (10 min TTL, máx 100 entradas) |
| Documentación API | Springdoc OpenAPI 3.1.1 / Swagger UI |
| Seguridad de contraseñas | BCrypt |
| Rate Limiting | Caffeine (login: 5 fallos / 60 s por IP y también por email) |

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
    │   │   ├── AppProperties.java      # Propiedades app.cors, app.jwt, app.admin, app.proxy
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
    │   ├── application-prod.yml          # Perfil prod: desactiva Swagger/OpenAPI
    │   └── db/migration/
    │       ├── V1__catalogo_inicial.sql      # Esquema + datos iniciales
    │       ├── V2__admin_usuarios.sql        # Tabla admin_usuarios
    │       ├── V3__servicio_imagenes.sql     # Imágenes/logos por servicio (bytea)
    │       └── V4__metodo_pago_imagenes.sql  # Imágenes por método de pago (bytea)
    │
    └── test/java/com/sheis/catalogo/
        ├── CatalogoApplicationTests.java
        ├── security/SecurityAndAdminTest.java
        ├── service/PricingServiceTest.java
        ├── web/CatalogoPublicoControllerTest.java
        ├── web/ServicioImagenIntegrationTest.java
        └── web/MetodoPagoImagenIntegrationTest.java
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
| V3 | `servicio_imagenes` — imágenes de servicios (`bytea`, PK `servicio_id` → `servicios`, `ON DELETE CASCADE`) | ✅ Aplicada |
| V4 | `metodo_pago_imagenes` — imágenes de métodos de pago (`bytea`, PK `metodo_pago_id` → `metodos_pago`, `ON DELETE CASCADE`) | ✅ Aplicada |

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

### Suite de tests (48/48 ✅)

| Clase | Tests | Cobertura |
|:--|:--|:--|
| `PricingServiceTest` | Unitarios | Cálculo de precios, descuentos progresivos, combos |
| `CatalogoPublicoControllerTest` | Contrato API | Formato JSON público, estructura, `ETag`/`Cache-Control` y 304 condicional |
| `ServicioImagenIntegrationTest` | Integración | Subir/borrar/consultar imagen de servicio, 401/404/400, cascada |
| `MetodoPagoImagenIntegrationTest` | Integración | Subir/borrar/consultar imagen de método de pago, 401/404/400, cascada |
| `SecurityAndAdminTest` | Integración | Auth 401/403/200, login válido/inválido, rate limiting (incl. no-bypass por `X-Forwarded-For`) |
| `Plan Estimation` y `Planes Validation` | Integración | Estimación/validación de planes |
| `CorsOriginsNormalizationTest` | Unitarios | Normalización de `CORS_ORIGINS` (barra final, espacios, saltos de línea, comas, mayúsculas, duplicados) y log INFO |
| `CorsConfigurationTest` | Integración CORS | Origen permitido/denegado, comparación case-insensitive, preflight `OPTIONS` de catálogo y login, log WARN |
| `CatalogoApplicationTests` | Contexto | Carga del contexto Spring |

**Resultado**: `Tests run: 48, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS`

**Nota**: los tests usan H2 con `ddl-auto: create-drop` y **Flyway deshabilitado**, por lo que el esquema lo
genera Hibernate desde las entidades y **no hay datos seed**; los tests de integración de imagen crean el
`Servicio`/`MetodoPago` que necesitan en `@BeforeEach` (no asumir que `nequi`/`nc` existan).

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

### DEC-006 — Imágenes de servicio y de método de pago en BD (bytea)
**Fecha:** 2026-10-16
**Decisión:** Guardar las imágenes como `bytea` en PostgreSQL detrás de una interfaz de almacén
(`ImagenServicioStore`/`ImagenMetodoPagoStore`, impl. `Postgres*Store`), tabla 1:1 con la entidad
(`servicio_imagenes` V3, `metodo_pago_imagenes` V4, ambas `ON DELETE CASCADE`). Validación por magic bytes
(PNG/JPEG/WebP) con `ImageValidator` y límite **512 KB** tras subir.
**Endpoints:** `PUT/DELETE /api/admin/{servicios/{codigo}|metodos-pago/{id}}/imagen` (multipart, rol ADMIN) y
`GET /api/public/{servicios/{codigo}|metodos-pago/{id}}/imagen` (`Cache-Control: immutable` + `nosniff`).
El DTO (`ServicioPublicoDto`/`AdminServicioDto`, `MetodoPagoDto`/`AdminMetodoPagoDto`) incluye `imagenUrl`
= `/api/public/.../imagen?v={epochMillis}` (o `null`), de modo que al cambiar la imagen cambia la URL.
Toda subida/borrado **invalida la caché Caffeine** (`@CacheEvict("catalogo", allEntries = true)`).
**Motivo:** El panel admin permite un logo por servicio y una imagen por método de pago (petición del usuario).

### DEC-007 — Endurecimiento de seguridad para producción
**Fecha:** 2026-10-16
**Decisión:** (1) `AuthController` deja de confiar en `X-Forwarded-For` por defecto; solo lo usa si
`app.proxy.trust-forwarded-headers=true` (var `TRUST_FORWARDED_HEADERS`), pensado únicamente para despliegues
detrás de un proxy de confianza que sobrescribe la cabecera. Así el rate limiter se ancla a la IP real del
socket y no puede eludirse falsificando la cabecera. (2) `AuthService` ejecuta siempre un `passwordEncoder.matches`
contra un hash BCrypt dummy cuando el correo no existe, evitando enumeración de usuarios por *timing*.
(3) `GlobalExceptionHandler` responde un mensaje fijo en 401 (no propaga el texto de la excepción de Security).
(4) Nuevo perfil `prod` (`application-prod.yml`) que desactiva Swagger/OpenAPI (`springdoc.api-docs.enabled=false`,
`springdoc.swagger-ui.enabled=false`) y el detalle del healthcheck.
**Motivo:** Auditoría de seguridad previa al despliegue a producción.
**Impacto:** `AuthController`, `AuthService`, `GlobalExceptionHandler`, `AppProperties` (nueva sección `app.proxy`),
`application.yml`, `application-prod.yml`, `.env.example`. En producción: `SPRING_PROFILES_ACTIVE=prod`.

### DEC-008 — Caché HTTP (ETag / Cache-Control) del catálogo público
**Fecha:** 2026-10-16
**Decisión:** `GET /api/public/catalogo` serializa la respuesta con el `JsonMapper` (Jackson 3) y añade
`ETag` (MD5 del JSON) + `Cache-Control: max-age=30, must-revalidate, public`. Si el cliente envía
`If-None-Match` coincidente, responde **304 Not Modified** sin cuerpo (revalidación barata). El front cachea
persistentemente en `localStorage` y revalida en segundo plano, apoyándose en este ETag.
**Nota técnica:** Spring Boot 4 usa **Jackson 3** (`tools.jackson.databind.json.JsonMapper`); el bean a inyectar
es `JsonMapper`, **no** el `ObjectMapper` de `com.fasterxml` (Jackson 2 sigue en el classpath por springdoc, pero
no hay bean de ese tipo). El controlador devuelve `byte[]` para evitar una doble serialización.
**Motivo:** Mejorar la experiencia del cliente (carga instantánea + revalidación barata) sin sacrificar frescura.
**Impacto:** `CatalogoPublicoController`, `CatalogoPublicoControllerTest`.

### DEC-009 — RLS de Supabase (V5) y lock de Flyway sobre su tabla de historial
**Fecha:** 2026-10-16
**Decisión:** Migración `V5__rls_supabase.sql` que ejecuta `ALTER TABLE ... ENABLE ROW LEVEL SECURITY`
sobre las 13 tablas del esquema `public` (incluida `flyway_schema_history`) **sin crear políticas**, y
revoca `ALL` a los roles `anon`/`authenticated` **solo si existen** (idempotente y seguro en H2/local).
El dueño de las tablas (usuario JDBC del backend) mantiene acceso pleno (no se activa `FORCE ROW LEVEL SECURITY`).
**Trampa crítica (resuelta):** con el **lock transaccional por defecto** de Flyway sobre PostgreSQL, la
propia migración mantiene bloqueada `flyway_schema_history` y el `ALTER` sobre esa tabla espera un lock
hasta agotar el `statement_timeout` de Supabase (120 s → SQLState 57014). Fuera de Flyway el `ALTER` es
instantáneo (~100 ms). **Solución:** en el perfil `prod`, `spring.flyway.postgresql.transactional-lock: false`
(lock a nivel de sesión) → V5 se aplica en <1 s. Verificado: Flyway v5 `success=true` y `relrowsecurity=true`
en las 13 tablas.
**Motivo:** Supabase expone el esquema `public` por PostgREST con la clave `anon`; sin RLS, cualquiera con
esa clave pública podría leer/escribir datos del negocio. El backend no usa PostgREST (JDBC directo).
**Impacto:** `V5__rls_supabase.sql`, `application-prod.yml`.

### DEC-010 — Endurecimiento y empaquetado para producción
**Fecha:** 2026-10-16
**Decisión:** (1) `AppProperties` valida **fail-fast** al arrancar: `JWT_SECRET` ≥ 32 caracteres y distinto de
valores de ejemplo; `ADMIN_EMAIL`/`ADMIN_PASSWORD` presentes y contraseña ≥ 10. (2) `application-prod.yml`
endurecido: puerto `${PORT:8082}`, `forward-headers-strategy: framework`, DB/CORS/JWT **solo por variables de
entorno sin default**, Hikari 5/1, `ddl-auto: validate`, Flyway on, Swagger desactivado, errores sin detalle,
logs INFO sin SQL, compresión JSON. (3) Cabeceras de seguridad: `Referrer-Policy: no-referrer` y
`Permissions-Policy` restrictivo (además de `nosniff`/`DENY` ya por defecto). (4) `LoginRateLimiter` reescrito a
**5 fallos / 60 s por IP y también por email** con mensaje genérico (no revela qué cuenta se bloqueó).
(5) `GlobalExceptionHandler` mapea 405 y 404 en vez de 500 genérico. (6) `Cache-Control` público pasa de
`max-age=30` a `max-age=60`. (7) `Dockerfile` multi-etapa (`maven:3.9-eclipse-temurin-25` →
`eclipse-temurin:25-jre-alpine`), usuario no root, `EXPOSE 8080`, `ENV PORT=8080`, `HEALTHCHECK` a
`/actuator/health`; plus `.dockerignore` y `DEPLOY.md`.
**Desviación reportada:** el requisito pedía CORS con métodos `GET, POST, PUT, DELETE, OPTIONS`, pero el panel
usa **PATCH** (`/visible`, `/stock`); se mantuvo PATCH para no romper funcionalidad.
**Motivo:** Dejar el backend listo para despliegue real (config por entorno, seguridad, empaquetado).
**Impacto:** `AppProperties`, `application.yml`, `application-prod.yml`, `SecurityConfig`, `LoginRateLimiter`,
`RateLimitException`, `AuthService`, `CatalogoPublicoController`, `GlobalExceptionHandler`, `DataInitializer`,
`Dockerfile`, `.dockerignore`, `DEPLOY.md`, `.env.example`, `.gitignore`.
**Verificado en local (perfil `prod` contra Supabase):** `/actuator/health` 200; `GET /api/public/catalogo`
200 con `ETag` + `Cache-Control: max-age=60`; `/api/admin/**` 401 sin token y 200 con token ADMIN; Swagger
(`/v3/api-docs`, `/swagger-ui`) no accesible (401 anón / 403 con sesión); 6.º login fallido → 429.
Tests backend **38/38 `BUILD SUCCESS`**.

### DEC-011 — Normalización de `CORS_ORIGINS` y diagnóstico de orígenes
**Fecha:** 2026-10-18
**Decisión:** `AppProperties.getAllowedOriginsList()` normaliza cada origen: separa por coma **o cualquier
espacio/salto de línea** (`split("[,\\s]+")`), recorta espacios, elimina barras finales (`/+$`), descarta
vacíos, pasa a minúsculas y elimina duplicados. Al arrancar registra en **INFO** la lista final
(`CORS: orígenes permitidos (N): [...]`). `NormalizedCorsConfiguration` (subclase de `CorsConfiguration`,
registrada en `SecurityConfig`) sobrescribe `checkOrigin` para comparar sin distinguir mayúsculas y
devolver el origen recibido si coincide; si no, registra **WARN** con el origen rechazado y la lista
permitida (`CORS: origen rechazado '...'`).
**Motivo:** Los paneles de despliegue (Render/Railway/Vercel) introducen espacios, saltos de línea, comas
finales y barras finales al pegar `CORS_ORIGINS`; una comparación literal rompía el CORS y el navegador
mostraba errores opacos. Los logs permiten diagnosticar sin adivinar.
**Trampa (tests):** un `\n` dentro de `@SpringBootTest(properties = ...)` se trunca al parsear; usar
`@DynamicPropertySource` para inyectar el valor multilínea real.
**Verificado (perfil `prod`, Supabase):** `CORS_ORIGINS` con espacios + barra final → INFO con 1 origen
normalizado; con salto de línea + mayúsculas + `///` + comas → INFO con 3 orígenes; `GET
/api/public/catalogo` 200 con `Access-Control-Allow-Origin` igual al origen; orígenes en minúsculas
aceptados contra config en mayúsculas; origen no permitido → 403 `Invalid CORS request` + WARN; preflight
`OPTIONS /api/public/catalogo` y `/api/auth/login` → 200 con `Allow-Methods`/`Allow-Headers` y
`Allow-Credentials: true`. Tests backend **48/48**.
**Impacto:** `AppProperties`, `security/NormalizedCorsConfiguration`, `SecurityConfig`,
`CorsOriginsNormalizationTest`, `CorsConfigurationTest`.

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
| Imágenes de servicio y de método de pago (V3/V4) | 2026-10-16 | `bytea` + endpoints admin/públicos (ver DEC-006) |
| Endurecimiento de seguridad para producción | 2026-10-16 | XFF no confiable por defecto, anti-timing, 401 fijo, perfil `prod` sin Swagger (ver DEC-007) |
| Caché HTTP del catálogo público (ETag/304) | 2026-10-16 | `GET /api/public/catalogo` con `ETag` + `Cache-Control` y 304 condicional (ver DEC-008) |
| RLS de Supabase (V5) en las 13 tablas | 2026-10-16 | Sin políticas; dueño JDBC exento; `transactional-lock=false` en prod (ver DEC-009) |
| Endurecimiento y empaquetado para producción | 2026-10-16 | Fail-fast secretos, perfil `prod`, rate limit 5/min, Dockerfile no-root, `DEPLOY.md` (ver DEC-010) |
| CORS tolerante a entornos de despliegue | 2026-10-18 | Normaliza `CORS_ORIGINS` (espacios, saltos, comas, barras, mayúsculas), logs INFO/WARN y preflight verificado (ver DEC-011) |

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

### 2026-10-18
- **CORS tolerante a entornos de despliegue (DEC-011)**: `CORS_ORIGINS` se normaliza (comas/espacios/saltos de línea, barras finales, mayúsculas, duplicados), log INFO al arrancar y WARN al rechazar un origen. Verificado con curl contra perfil `prod`: ACAO exacto, preflight 200 en catálogo y login, 403 + WARN para origen no permitido. Tests **48/48**.

### 2026-10-16
- **Listo para producción (DEC-009/DEC-010)**: migración RLS `V5` aplicada en Supabase (`transactional-lock=false` en `prod`), fail-fast de secretos, perfil `prod` endurecido, rate limit 5/min, `Dockerfile` multi-etapa no-root, `DEPLOY.md`. Verificado en local con perfil `prod`: health 200, catálogo ETag 60 s, admin 401, Swagger inaccesible, 6.º login → 429. Tests **38/38**.
- **Caché HTTP del catálogo público (DEC-008)**: `GET /api/public/catalogo` con `ETag` + `Cache-Control` y 304 condicional (`JsonMapper` de Jackson 3).
- **Imágenes de método de pago (V4)**: tabla `metodo_pago_imagenes`, endpoints admin/públicos, `imagenUrl` en DTOs (DEC-006).
- **Auditoría de seguridad y endurecimiento para producción (DEC-007)**: rate limiter anclado a IP real (sin confiar en `X-Forwarded-For`), mitigación de enumeración por timing, 401 con mensaje fijo, perfil `prod` que desactiva Swagger/OpenAPI.
- **Tests**: 29/29 `BUILD SUCCESS` (nuevo test de no-bypass del rate limiter vía `X-Forwarded-For`).

### 2026-10-15
- **Proyecto creado desde cero**: Backend completo con Spring Boot 4.1.1, Java 25, PostgreSQL + Flyway, H2 para tests.
- **Arquitectura**: Controller → Service → Repository → DB. OAuth2 Resource Server con JWT HS256.
- **19 tests exitosos**: PricingService, CatalogoPublico, Security, contexto Spring.
- **Documentación**: README.md, docs/api.md, .env.example.
- **Resolución**: Incompatibilidad Lombok + Java 25 (constructores manuales).
- **Resolución**: Incompatibilidad H2 + `columnDefinition = "text[]"` (removido).
- **Resolución**: Spring Boot 4.x `AutoConfigureMockMvc` (MockMvc manual).
