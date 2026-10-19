# DEPLOY.md — Despliegue del Backend Catálogo TiendaSheis

Guía para llevar el backend a producción (Spring Boot 4.1.1 / Java 25, PostgreSQL, perfil `prod`).

---

## 1. Requisitos

- Java 25 (solo para ejecución local o build con Maven wrapper).
- PostgreSQL 15+ (en producción se usa un proyecto de **Supabase**).
- Docker (opcional, si se despliega en contenedor).
- Maven wrapper incluido (`mvnw` / `mvnw.cmd`). No requiere Maven instalado.

---

## 2. Variables de entorno

El perfil `prod` **no arranca** si falta alguno de los valores obligatorios.

| Variable | Obligatoria | Descripción |
|:--|:--:|:--|
| `SPRING_PROFILES_ACTIVE` | Sí | Usar `prod` en producción. |
| `DB_URL` | Sí | JDBC de PostgreSQL. En Supabase, la cadena del *pooler* con `?sslmode=require`. |
| `DB_USER` | Sí | Usuario de la base de datos (dueño de las tablas). |
| `DB_PASSWORD` | Sí | Contraseña del usuario de la base de datos. |
| `JWT_SECRET` | Sí | Secreto aleatorio para firmar JWT. **Mínimo 32 caracteres.** No usar valores de ejemplo (`openssl rand -base64 48`). |
| `JWT_EXPIRATION_MINUTES` | No | Caducidad del token en minutos. Por defecto `480` (8 horas). |
| `ADMIN_EMAIL` | Sí | Correo del administrador inicial. |
| `ADMIN_PASSWORD` | Sí | Contraseña del administrador inicial. **Mínimo 10 caracteres.** |
| `ADMIN_RESET_PASSWORD` | No | `true` para restablecer la contraseña del admin al arrancar. **Usar una sola vez** y luego eliminarla. Por defecto `false`. |
| `CORS_ORIGINS` | Sí | Orígenes permitidos del frontend, separados por comas (p. ej. `https://tudominio.com,https://www.tudominio.com`). Sin esta variable la app no arranca. |
| `PORT` | No | Puerto de escucha. Por defecto `8082` (en la imagen Docker es `8080`). |
| `TRUST_FORWARDED_HEADERS` | No | `true` **solo** detrás de un proxy de confianza que sobrescriba `X-Forwarded-For`. Por defecto `false`. |

> **Secretos**: nunca se guardan en el repositorio. `.env` está en `.gitignore`; `.env.example` solo contiene marcadores.

---

## 3. Arranque seguro (fail-fast)

Al iniciar, `AppProperties#validate()` verifica:

- `JWT_SECRET` presente, ≥ 32 caracteres y distinto de valores de ejemplo/conocidos.
- `ADMIN_EMAIL` y `ADMIN_PASSWORD` presentes y con contraseña ≥ 10 caracteres.

Si algo falla, la aplicación **no arranca** y registra el motivo exacto.

---

## 4. Construir y ejecutar

### 4.1. Local / servidor con JAR

```bash
# Construir el JAR ejecutable (los tests no se ejecutan aquí)
./mvnw clean package -DskipTests

# Ejecutar en modo producción
SPRING_PROFILES_ACTIVE=prod \
DB_URL="jdbc:postgresql://db:5432/sheis_catalogo?sslmode=require" \
DB_USER="usuario_bd" \
DB_PASSWORD="********" \
JWT_SECRET="$(openssl rand -base64 48)" \
ADMIN_EMAIL="admin@tudominio.com" \
ADMIN_PASSWORD="**********" \
CORS_ORIGINS="https://tudominio.com" \
java -jar target/catalogo-0.0.1-SNAPSHOT.jar
```

En Windows PowerShell, define cada variable con `$env:NOMBRE="valor"` antes de `java -jar`.

### 4.2. Docker

```bash
# Construir la imagen (multi-etapa: Maven+JDK 25 -> JRE 25 alpine, usuario no root)
docker build -t tiendasheis-backend:latest .

# Ejecutar (puerto 8080 del contenedor; usa --env-file o -e por variable)
docker run -d --name sheis-backend -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL="jdbc:postgresql://host.docker.internal:5432/sheis_catalogo?sslmode=require" \
  -e DB_USER="usuario_bd" \
  -e DB_PASSWORD="********" \
  -e JWT_SECRET="******** secreto de 32+ caracteres ********" \
  -e ADMIN_EMAIL="admin@tudominio.com" \
  -e ADMIN_PASSWORD="**********" \
  -e CORS_ORIGINS="https://tudominio.com" \
  tiendasheis-backend:latest

# Verificar salud
curl http://localhost:8080/actuator/health
```

---

## 5. Primer administrador

- Se crea **automáticamente** en el primer arranque, **solo** si la tabla `admin_usuarios` está **vacía**, usando `ADMIN_EMAIL` / `ADMIN_PASSWORD`.
- Si ya existe al menos un usuario, **no se crea ni se sobrescribe nada** (cambiar `ADMIN_PASSWORD` luego no altera la contraseña existente).
- Para restablecer una contraseña olvidada, usa el mecanismo opcional de la sección 5.1.

### 5.1. Restablecimiento opcional de la contraseña

Si necesitas recuperar el acceso al panel:

1. Define temporalmente `ADMIN_RESET_PASSWORD=true` junto con `ADMIN_EMAIL` y `ADMIN_PASSWORD` (≥ 10 caracteres) en el servicio.
2. Reinicia el backend. Al arrancar se actualiza el hash del admin cuyo correo coincide con `ADMIN_EMAIL`; si ese correo no existe, se crea.
3. El log muestra `Contraseña del admin restablecida; elimina ADMIN_RESET_PASSWORD` (nunca se registra la contraseña ni el hash).
4. Elimina `ADMIN_RESET_PASSWORD` (o ponlo en `false`) y reinicia.

Si la variable no existe o no vale `true`, no se modifica ninguna contraseña.

---

## 6. Migraciones (Flyway)

- Ubicación: `src/main/resources/db/migration/` (`V{n}__descripcion.sql`).
- **REGLA DE ORO: nunca editar una migración ya aplicada.** Cualquier cambio de esquema o de RLS se añade como una migración **nueva**.
- Última versión actual: **V5** (activa RLS para Supabase).
- `baseline-on-migrate` está activo; en una base existente se marca la versión base y se aplican las siguientes.

### RLS (Supabase)

La migración `V5__rls_supabase.sql` activa **Row Level Security sin políticas** en todas las tablas (incluidas `admin_usuarios`, `servicio_imagenes`, `metodo_pago_imagenes` y `flyway_schema_history`). Así, la API REST pública de Supabase (roles `anon`/`authenticated`) no puede leer ni escribir nada. El backend no se ve afectado porque su usuario JDBC es **dueño** de las tablas (en PostgreSQL el dueño está exento de RLS salvo `FORCE ROW LEVEL SECURITY`, que no se activa).

**Comprobación** (con `psql`, como dueño de las tablas):

```sql
-- Todas deben salir con relrowsecurity = true
SELECT relname, relrowsecurity
FROM pg_class
WHERE relname IN ('servicios','sku_categorias','tarifas','descuentos_combo','combos',
                  'combo_items','metodos_pago','horarios','catalogo_config','admin_usuarios',
                  'servicio_imagenes','metodo_pago_imagenes','flyway_schema_history')
ORDER BY relname;
```

Y con la API pública de Supabase (clave `anon`), un `GET` a cualquiera de esas tablas debe devolver **vacío/403**; el backend en cambio sigue leyendo y escribiendo con normalidad.

---

## 7. Endpoints de verificación

| Endpoint | Esperado |
|:--|:--|
| `GET /actuator/health` | `200 {"status":"UP"}` (público) |
| `GET /api/public/catalogo` | `200` con `ETag` y `Cache-Control: public, max-age=60`; `304` al revalidar |
| `GET /api/admin/**` sin token | `401` |
| `POST /api/auth/login` (6 fallos en 1 min) | `429` |
| Swagger (`/swagger-ui.html`) en prod | No existe (404) |

---

## 8. Respaldos

**PostgreSQL / Supabase** (desde una máquina con `pg_dump`):

```bash
# Respaldo
pg_dump "postgresql://usuario:password@host:5432/sheis_catalogo?sslmode=require" \
  --format=custom --no-owner --file=respaldo_$(date +%Y%m%d).dump

# Restauración (en una base vacía)
pg_restore --clean --if-exists --no-owner \
  --dbname="postgresql://usuario:password@host:5432/sheis_catalogo?sslmode=require" \
  respaldo_YYYYMMDD.dump
```

En Supabase también puedes usar **Database → Backups** (respaldos automáticos diarios y *Point-in-Time Recovery* en planes de pago). Haz copias antes de migraciones importantes.

---

## 9. Consideraciones de escala

- **Una sola instancia por ahora.** La caché del catálogo (Caffeine) y el límite de intentos de login viven **en memoria**; con varias instancias cada una tiene su propia caché y su propio contador. Para escalar horizontalmente habría que moverlos a un almacén compartido (p. ej. Redis).
- El pool de Hikari está limitado a `maximum-pool-size: 5` porque el *pooler* de Supabase limita las conexiones.
- Compresión HTTP activada para JSON.
