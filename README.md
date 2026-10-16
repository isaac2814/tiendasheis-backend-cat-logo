# Backend Catálogo - TiendaSheis

API REST y motor de precios para el catálogo de streaming de TiendaSheis. Desarrollado con Spring Boot, Java y PostgreSQL.

---

## 1. Requisitos del Sistema

- **Java Development Kit (JDK)**: Versión 25 (o superior compatible).
- **PostgreSQL**: Versión 15+ (local o en la nube como Supabase/Neon).
- **Maven**: Wrapper incluido (`mvnw` / `mvnw.cmd`).

---

## 2. Configuración y Base de Datos

### 2.1 Crear la Base de Datos en PostgreSQL
Conéctate a tu servidor PostgreSQL (mediante `psql`, pgAdmin o DBeaver) y crea la base de datos:

```sql
CREATE DATABASE tiendasheis;
```

> **Nota sobre Migraciones:** No es necesario crear las tablas manualmente. Flyway ejecuta automáticamente las migraciones versionadas en `src/main/resources/db/migration/`:
> - `V1__catalogo_inicial.sql`: Esquema del catálogo (servicios, sku_categorias, tarifas, combos, etc.) e inserción de datos iniciales.
> - `V2__admin_usuarios.sql`: Tabla de usuarios administradores con credenciales seguras.

### 2.2 Configuración de Variables de Entorno (`.env`)
En la raíz de la carpeta `Backend catalogo/`, copia el archivo `.env.example` como `.env`:

```bash
cp .env.example .env
```

Configura tus valores:

```properties
# Base de Datos
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tiendasheis
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=tu_password_aqui

# Puerto del Servidor
SERVER_PORT=8080

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000

# Seguridad JWT (mínimo 32 caracteres)
JWT_SECRET=tu_secreto_super_seguro_de_al_menos_32_caracteres_xyz123
JWT_TTL_HOURS=24

# Administrador Inicial (creado automáticamente si la tabla está vacía)
ADMIN_EMAIL=admin@tiendasheis.com
ADMIN_PASSWORD=PasswordSeguro123!
```

---

## 3. Ejecución en Desarrollo

Para iniciar el backend en modo desarrollo:

```bash
# En Windows:
.\mvnw.cmd spring-boot:run

# En Linux/macOS:
./mvnw spring-boot:run
```

El servidor iniciará en `http://localhost:8080`.

---

## 4. Creación del Primer Administrador

El sistema cuenta con un `ApplicationRunner` (`DataInitializer`) que se ejecuta al arrancar:
1. Comprueba si la tabla `admin_usuarios` está vacía.
2. Si está vacía y se configuraron las variables `ADMIN_EMAIL` y `ADMIN_PASSWORD` (con al menos 10 caracteres), crea automáticamente el primer usuario administrador con la contraseña hasheada mediante **BCrypt**.
3. Si la base de datos ya contiene al menos un administrador, `DataInitializer` no realiza ninguna acción.

---

## 5. Documentación Interactiva (Swagger UI)

Una vez iniciado el servidor, puedes explorar y probar todos los endpoints desde el navegador:

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Para probar los endpoints protegidos (`/api/admin/**`) en Swagger UI:
1. Llama al endpoint `POST /api/auth/login` con tus credenciales.
2. Copia el `token` de la respuesta.
3. Haz clic en el botón superior derecho **Authorize** (candado) e ingresa:
   ```text
   Bearer <tu_token>
   ```

Para una referencia detallada de contratos JSON y ejemplos de `curl`, consulta [docs/api.md](docs/api.md).

---

## 6. Pruebas Automatizadas

Para ejecutar toda la suite de pruebas automatizadas:

```bash
.\mvnw.cmd test
```

La suite cubre:
- **`PricingServiceTest`**: Pruebas unitarias de cálculo de precios base, descuentos por volumen progresivos y combos.
- **`CatalogoPublicoControllerTest`**: Pruebas de contrato y formato JSON para clientes públicos.
- **`PlanesValidationTest`**: Pruebas unitarias de validación de reglas de negocio para planes (precios > 0, prevención de combinaciones duplicadas de días y perfiles).
- **`SecurityAndAdminTest`**: Pruebas de integración con MockMvc que verifican protección de rutas (401 sin token, 403 con rol no autorizado, 200 con ADMIN) y login con credenciales válidas e inválidas.

---

## 7. Decisiones Técnicas y Arquitectura

### 7.1 H2 en Memoria para Pruebas (Sin Dependencia de Docker)
Para facilitar el desarrollo y la ejecución de tests en cualquier entorno sin necesidad de tener Docker o Testcontainers ejecutándose, el perfil de pruebas (`src/test/resources/application.yml`) utiliza una base de datos **H2 en memoria** configurada con `MODE=PostgreSQL`. Todas las entidades JPA fueron diseñadas de forma estándar para ser 100% compatibles tanto con PostgreSQL nativo en producción como con H2 en las pruebas.

### 7.2 Arquitectura Desacoplada de Stock (`StockProvider`)
La gestión de disponibilidad y stock de cuentas de streaming está desacoplada mediante la interfaz `StockProvider`:
- La implementación por defecto es `ManualStockProvider`, que lee el campo `stock_manual` de cada servicio.
- Si en el futuro se implementa un sistema automatizado de inventario de cuentas (ej. stock físico de credenciales por perfiles en base de datos o servicio externo), bastará con crear una nueva clase anotada con `@Primary` o `@Component` que implemente `StockProvider`, sin modificar ninguna línea de los servicios de catálogo ni de los controladores.

### 7.3 Arquitectura de Autenticación Preparada para Supabase Auth
El flujo de autenticación y autorización se implementó sobre el estándar **OAuth 2.0 Resource Server** de Spring Security:
- Actualmente emite y valida tokens JWT firmados localmente con HS256 (`TokenService` y `NimbusJwtEncoder`/`NimbusJwtDecoder`).
- Cuando se decida migrar la autenticación a **Supabase Auth**, solo es necesario actualizar `SecurityConfig` para apuntar el `JwtDecoder` al endpoint JWKS público de Supabase (`https://<project-ref>.supabase.co/auth/v1/.well-known/jwks.json`). Toda la lógica de controladores, servicios de dominio y decoradores `@SecurityRequirement` se mantendrá idéntica y sin cambios.
