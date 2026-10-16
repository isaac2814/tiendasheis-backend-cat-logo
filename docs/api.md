# Catálogo TiendaSheis - Documentación de la API REST

Este documento describe la especificación técnica completa y ejemplos de uso de la API REST del Catálogo de TiendaSheis.

---

## 1. Convenciones y Autenticación

- **Base URL Local**: `http://localhost:8080`
- **Swagger UI Interactivo**: `http://localhost:8080/swagger-ui.html` *(deshabilitado con el perfil `prod`)*
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs` *(deshabilitado con el perfil `prod`)*
- **Formato de datos**: `application/json; charset=UTF-8`
- **Formato de errores**: RFC 9457 `ProblemDetail` (application/problem+json)

### Autenticación
Los endpoints `/api/admin/**` requieren autenticación mediante JWT (JSON Web Token) con algoritmo **HS256**.
El token se debe enviar en el encabezado HTTP:
```http
Authorization: Bearer <TOKEN_JWT>
```

---

## 2. Formato Estándar de Errores (RFC 9457)

Todos los errores del sistema retornan una estructura estándar `ProblemDetail`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "El precio debe ser mayor a 0",
  "instance": "/api/admin/servicios/nc/planes"
}
```

### Códigos de estado HTTP:
- **400 Bad Request**: Validación de campos fallida o lógica de negocio inválida (ej. combinaciones duplicadas de días y perfiles).
- **401 Unauthorized**: Falta token de autenticación o credenciales de inicio de sesión inválidas.
- **403 Forbidden**: Token válido pero sin rol `ADMIN`.
- **404 Not Found**: Recurso no encontrado (ej. código de servicio no existe).
- **409 Conflict**: Conflicto de unicidad (ej. código de servicio ya existe).
- **429 Too Many Requests**: Límite de tasa excedido en inicio de sesión (máximo 5 intentos fallidos por IP/email en 15 minutos).
- **500 Internal Server Error**: Error no controlado del servidor.

---

## 3. Endpoints Públicos

### 3.1 Obtener Catálogo Público Completo
Retorna los servicios visibles con sus planes activos, precio base calculado, combos activos, descuentos por volumen calculados dinámicamente, métodos de pago activos y horarios de atención.

- **Método**: `GET`
- **Ruta**: `/api/public/catalogo`
- **Autenticación**: Ninguna (público)
- **Caché**: Cacheado en memoria (Caffeine). Se invalida automáticamente ante cualquier mutación desde el panel de administración.

#### Respuesta Exitosa (200 OK):
```json
{
  "servicios": [
    {
      "id": "nc",
      "nombre": "Netflix",
      "inicial": "N",
      "color": "#8B1A1A",
      "logoUrl": "https://example.com/netflix.png",
      "modalidad": "Perfil compartido",
      "descripcion": "Disfruta de las mejores series y películas en Ultra HD.",
      "incluye": [
        "1 pantalla / perfil exclusivo",
        "Calidad 4K Ultra HD",
        "PIN de seguridad personalizado"
      ],
      "recomendaciones": [
        "No modificar el correo ni la clave",
        "Uso exclusivo en 1 dispositivo simultáneo"
      ],
      "stock": 5,
      "agotado": false,
      "precioBase": 14400,
      "planes": [
        {
          "dias": 30,
          "perfiles": 1,
          "precio": 14400
        }
      ]
    }
  ],
  "combos": [
    {
      "id": 1,
      "etiqueta": "Dúo Streaming Popular",
      "servicios": ["nc", "de"],
      "precioNormal": 21900,
      "descuento": 2500,
      "precio": 19400,
      "agotado": false
    }
  ],
  "descuentos": {
    "porCantidad": {
      "2": 2500,
      "3": 5000,
      "4": 7000,
      "5": 9000
    },
    "extraDesdeSexto": 2000
  },
  "metodosPago": [
    {
      "id": "nequi",
      "nombre": "Nequi",
      "inicial": "N",
      "color": "#6A2C91",
      "etiquetaDato": "Número de Nequi",
      "dato": "3167846813",
      "titular": "Tienda Sheis Oficial"
    }
  ],
  "horario": [
    {
      "dia": "Lunes a viernes",
      "texto": "8:00 AM - 10:00 PM"
    },
    {
      "dia": "Sábados y domingos",
      "texto": "9:00 AM - 9:00 PM"
    }
  ],
  "whatsapp": "57 316 784 6813",
  "sobreNosotros": "En Tienda Sheis ofrecemos cuentas de streaming seguras, garantizadas y con soporte inmediato."
}
```

#### Ejemplo de cURL:
```bash
curl -X GET http://localhost:8080/api/public/catalogo
```

#### Caché HTTP:
La respuesta incluye `ETag` (hash MD5 del JSON) y `Cache-Control: max-age=30, must-revalidate, public`.
Si el cliente repite la petición con `If-None-Match` y el catálogo no cambió, el servidor responde
**`304 Not Modified`** sin cuerpo (revalidación barata):
```bash
curl -X GET http://localhost:8080/api/public/catalogo \
  -H 'If-None-Match: "42bcca426ab42c1fb77b5f0ba9d71daa"'
```

---

## 4. Endpoints de Autenticación

### 4.1 Iniciar Sesión de Administrador
Valida credenciales contra la base de datos y genera un token JWT con rol `ADMIN`.

- **Método**: `POST`
- **Ruta**: `/api/auth/login`
- **Autenticación**: Ninguna
- **Rate Limit**: 5 intentos fallidos cada 15 minutos por IP + correo.

#### Cuerpo de la Petición:
```json
{
  "email": "admin@tiendasheis.com",
  "password": "miPasswordSeguro123"
}
```

#### Respuesta Exitosa (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tipo": "Bearer",
  "expiraEnSegundos": 86400
}
```

#### Ejemplo de cURL:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@tiendasheis.com","password":"miPasswordSeguro123"}'
```

---

## 5. Endpoints de Administración (`/api/admin/**`)

> Todos los siguientes endpoints requieren el encabezado:
> `Authorization: Bearer <TOKEN_ADMIN>`

### 5.1 Snapshot Completo de Administración
Retorna el inventario completo, incluyendo servicios ocultos y planes inactivos.

- **Método**: `GET`
- **Ruta**: `/api/admin/catalogo`

#### Ejemplo de cURL:
```bash
curl -X GET http://localhost:8080/api/admin/catalogo \
  -H "Authorization: Bearer $TOKEN"
```

---

### 5.2 Crear Nuevo Servicio
Registra un servicio en el catálogo. Si no se envía `codigo`, el sistema genera automáticamente un slug único.

- **Método**: `POST`
- **Ruta**: `/api/admin/servicios`

#### Cuerpo de la Petición:
```json
{
  "codigo": "max-estandar",
  "nombre": "Max Estándar",
  "modalidad": "Cuenta Completa",
  "inicial": "M",
  "color": "#002BE7",
  "logoUrl": "https://example.com/max.png",
  "etiqueta": "Nuevo",
  "descripcion": "Acceso a todo el catálogo de HBO y Warner Bros.",
  "incluye": [
    "2 pantallas simultáneas",
    "Resolución Full HD",
    "Descargas disponibles"
  ],
  "recomendaciones": [
    "No cambiar contraseña"
  ],
  "visible": true,
  "stockManual": 10,
  "planes": [
    {
      "dias": 30,
      "perfiles": 2,
      "precio": 18000,
      "activo": true
    }
  ]
}
```

#### Ejemplo de cURL:
```bash
curl -X POST http://localhost:8080/api/admin/servicios \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Max Estándar",
    "modalidad": "Cuenta Completa",
    "inicial": "M",
    "color": "#002BE7",
    "descripcion": "Acceso a todo el catálogo de HBO.",
    "planes": [{"dias": 30, "perfiles": 2, "precio": 18000, "activo": true}]
  }'
```

---

### 5.3 Actualizar Metadatos del Servicio
Actualiza nombre, colores, viñetas, recomendaciones y textos de un servicio existente.

- **Método**: `PUT`
- **Ruta**: `/api/admin/servicios/{id}`

#### Cuerpo de la Petición:
```json
{
  "nombre": "Max Estándar Full",
  "modalidad": "Cuenta Completa",
  "inicial": "M",
  "color": "#002BE7",
  "logoUrl": "https://example.com/max.png",
  "etiqueta": "Recomendado",
  "descripcion": "Acceso a todo el catálogo de HBO y Warner Bros en 4K.",
  "incluye": ["3 dispositivos simultáneos"],
  "recomendaciones": ["Garantía permanente"]
}
```

#### Ejemplo de cURL:
```bash
curl -X PUT http://localhost:8080/api/admin/servicios/max-estandar \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Max Estándar Full",
    "modalidad": "Cuenta Completa",
    "inicial": "M",
    "color": "#002BE7",
    "incluye": ["3 dispositivos"]
  }'
```

---

### 5.4 Alternar Visibilidad de Servicio
Oculta o muestra un servicio en el catálogo público sin eliminarlo de la base de datos.

- **Método**: `PATCH`
- **Ruta**: `/api/admin/servicios/{id}/visible`

#### Cuerpo de la Petición:
```json
{
  "visible": false
}
```

#### Ejemplo de cURL:
```bash
curl -X PATCH http://localhost:8080/api/admin/servicios/nc/visible \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"visible": false}'
```

---

### 5.5 Modificar Stock Manual
Define el stock disponible o `null` si no se desea controlar stock para ese servicio.

- **Método**: `PATCH`
- **Ruta**: `/api/admin/servicios/{id}/stock`

#### Cuerpo de la Petición:
```json
{
  "stock": 0
}
```

#### Ejemplo de cURL:
```bash
curl -X PATCH http://localhost:8080/api/admin/servicios/nc/stock \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"stock": 0}'
```

---

### 5.6 Eliminar Servicio
Elimina un servicio, sus tarifas y desvincula sus referencias de los combos existentes de forma segura.

- **Método**: `DELETE`
- **Ruta**: `/api/admin/servicios/{id}`

#### Respuesta Exitosa (200 OK):
```json
{
  "mensaje": "Servicio nc eliminado correctamente",
  "combosAfectados": 1
}
```

#### Ejemplo de cURL:
```bash
curl -X DELETE http://localhost:8080/api/admin/servicios/nc \
  -H "Authorization: Bearer $TOKEN"
```

---

### 5.7 Reemplazar Planes de un Servicio
Sobrescribe completamente la lista de planes (días, perfiles, precio, activo) de un servicio. Valida precios >= 0 y prohíbe duplicados en la tupla `(dias, perfiles)`.

- **Método**: `PUT`
- **Ruta**: `/api/admin/servicios/{id}/planes`

#### Cuerpo de la Petición:
```json
[
  {
    "dias": 30,
    "perfiles": 1,
    "precio": 14400,
    "activo": true
  },
  {
    "dias": 60,
    "perfiles": 1,
    "precio": 27000,
    "activo": true
  }
]
```

#### Ejemplo de cURL:
```bash
curl -X PUT http://localhost:8080/api/admin/servicios/nc/planes \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {"dias": 30, "perfiles": 1, "precio": 14400, "activo": true},
    {"dias": 60, "perfiles": 1, "precio": 27000, "activo": true}
  ]'
```

---

### 5.8 Reemplazar Lista de Combos
Actualiza los combos recomendados del catálogo y sus servicios asociados.

- **Método**: `PUT`
- **Ruta**: `/api/admin/combos`

#### Cuerpo de la Petición:
```json
[
  {
    "etiqueta": "Dúo Estelar",
    "servicios": ["nc", "de"],
    "activo": true,
    "orden": 1
  }
]
```

#### Ejemplo de cURL:
```bash
curl -X PUT http://localhost:8080/api/admin/combos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {"etiqueta": "Dúo Estelar", "servicios": ["nc", "de"], "activo": true, "orden": 1}
  ]'
```

---

### 5.9 Reemplazar Métodos de Pago
Actualiza las cuentas receptoras de pago (Nequi, Bancolombia, Daviplata, etc.).

- **Método**: `PUT`
- **Ruta**: `/api/admin/metodos-pago`

#### Cuerpo de la Petición:
```json
[
  {
    "id": "nequi",
    "nombre": "Nequi",
    "inicial": "N",
    "color": "#6A2C91",
    "etiquetaDato": "Número de Nequi",
    "dato": "3167846813",
    "titular": "Tienda Sheis",
    "activo": true,
    "orden": 1
  }
]
```

#### Ejemplo de cURL:
```bash
curl -X PUT http://localhost:8080/api/admin/metodos-pago \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {"id": "nequi", "nombre": "Nequi", "inicial": "N", "color": "#6A2C91", "etiquetaDato": "Número", "dato": "3167846813", "titular": "Tienda Sheis", "activo": true, "orden": 1}
  ]'
```

---

### 5.9.1 Imagen de un servicio
Sube, elimina o consulta el logo de un servicio (PNG/JPEG/WebP, máx. 512 KB).

- **Subir / reemplazar**: `PUT /api/admin/servicios/{codigo}/imagen` (multipart, campo `archivo`) → `200` con `{"imagenUrl": "..."}`; `400` si el archivo no es imagen válida o excede el tamaño; `404` si el servicio no existe; `401` sin token.
- **Eliminar**: `DELETE /api/admin/servicios/{codigo}/imagen` → `204`.
- **Consultar (público)**: `GET /api/public/servicios/{codigo}/imagen` → bytes con `Cache-Control: immutable`.

```bash
curl -X PUT http://localhost:8082/api/admin/servicios/nc/imagen \
  -H "Authorization: Bearer $TOKEN" \
  -F "archivo=@logo.png;type=image/png"
```

### 5.9.2 Imagen de un método de pago
Sube, elimina o consulta la imagen de un método de pago (PNG/JPEG/WebP, máx. 512 KB).

- **Subir / reemplazar**: `PUT /api/admin/metodos-pago/{id}/imagen` (multipart, campo `archivo`) → `200` con `{"imagenUrl": "..."}`; `400` si el archivo no es imagen válida o excede el tamaño; `404` si el método no existe; `401` sin token.
- **Eliminar**: `DELETE /api/admin/metodos-pago/{id}/imagen` → `204`.
- **Consultar (público)**: `GET /api/public/metodos-pago/{id}/imagen` → bytes con `Cache-Control: immutable`.

```bash
curl -X PUT http://localhost:8082/api/admin/metodos-pago/nequi/imagen \
  -H "Authorization: Bearer $TOKEN" \
  -F "archivo=@nequi.png;type=image/png"
```

### 5.10 Guardar Ajustes Generales y Descuentos
Actualiza teléfono de WhatsApp, textos informativos, escala de descuentos por volumen y horarios.

- **Método**: `PUT`
- **Ruta**: `/api/admin/ajustes`

#### Cuerpo de la Petición:
```json
{
  "whatsapp": "57 316 784 6813",
  "sobreNosotros": "Somos líderes en entretenimiento digital garantizado.",
  "descuentos": {
    "porCantidad": {
      "2": 2500,
      "3": 5000,
      "4": 7000,
      "5": 9000
    },
    "extraDesdeSexto": 2000
  },
  "horarios": [
    {
      "dia": "Lunes a viernes",
      "texto": "8:00 AM - 10:00 PM"
    },
    {
      "dia": "Sábados y domingos",
      "texto": "9:00 AM - 9:00 PM"
    }
  ]
}
```

#### Ejemplo de cURL:
```bash
curl -X PUT http://localhost:8080/api/admin/ajustes \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "whatsapp": "57 316 784 6813",
    "sobreNosotros": "Entretenimiento garantizado.",
    "descuentos": {"porCantidad": {"2": 2500, "3": 5000}, "extraDesdeSexto": 2000},
    "horarios": [{"dia": "Todos los días", "texto": "24/7"}]
  }'
```
