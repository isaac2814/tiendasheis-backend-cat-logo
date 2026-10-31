-- ============================================================================
-- V7: Tienda de productos tecnológicos físicos (Categorías, Productos, Variantes, Imágenes)
-- ============================================================================
-- Migración aditiva y segura para producción (Supabase / PostgreSQL y H2).
-- ============================================================================

-- 1) Tablas nuevas
CREATE TABLE IF NOT EXISTS categoria_producto (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(40) NOT NULL UNIQUE,
    orden INT NOT NULL DEFAULT 1,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS producto (
    id BIGSERIAL PRIMARY KEY,
    categoria_id BIGINT NOT NULL REFERENCES categoria_producto(id),
    nombre VARCHAR(80) NOT NULL,
    descripcion TEXT,
    incluye TEXT,
    garantia VARCHAR(80),
    lema VARCHAR(30),
    etiqueta_variante VARCHAR(30) NOT NULL DEFAULT 'Opción',
    orden INT NOT NULL DEFAULT 1,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS producto_variante (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL REFERENCES producto(id) ON DELETE CASCADE,
    nombre VARCHAR(60) NOT NULL,
    sku VARCHAR(50) NOT NULL UNIQUE,
    precio NUMERIC(12,2) NOT NULL CHECK (precio > 0),
    stock INT NOT NULL DEFAULT 0 CHECK (stock >= 0),
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    orden INT NOT NULL DEFAULT 1,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_producto_variante_nombre UNIQUE (producto_id, nombre)
);

CREATE TABLE IF NOT EXISTS producto_imagen (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL REFERENCES producto(id) ON DELETE CASCADE,
    orden INT NOT NULL DEFAULT 1,
    tipo VARCHAR(20) NOT NULL,
    contenido BYTEA NOT NULL,
    bytes_len INT NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 2) Datos iniciales de categorías
INSERT INTO categoria_producto (nombre, orden, visible) VALUES
    ('Audífonos', 1, TRUE),
    ('Forros', 2, TRUE),
    ('Cargadores', 3, TRUE),
    ('Accesorios', 4, TRUE)
ON CONFLICT (nombre) DO NOTHING;

-- 3) Textos por defecto en catalogo_config
INSERT INTO catalogo_config (clave, valor) VALUES
    ('titulo_tecnologia', 'Tecnología'),
    ('menu_tecnologia', 'Tecnología'),
    ('subtitulo_tecnologia', 'Audífonos, forros, cargadores y más. Envíos a todo el país.'),
    ('ciudad_domicilio', ''),
    ('texto_domicilio', 'Te lo llevamos a la puerta de tu casa.'),
    ('texto_envio_nacional', 'Por transportadora a cualquier ciudad. El costo te lo confirmamos por WhatsApp.'),
    ('nota_tecnologia', 'Pide por WhatsApp. Domicilio en tu ciudad o envío nacional; el costo del envío te lo confirmamos en el chat.')
ON CONFLICT (clave) DO NOTHING;

-- 4) Activar RLS en las nuevas tablas
DO $$
DECLARE
    tabla  TEXT;
    tablas TEXT[] := ARRAY[
        'categoria_producto',
        'producto',
        'producto_variante',
        'producto_imagen'
    ];
BEGIN
    FOREACH tabla IN ARRAY tablas LOOP
        IF to_regclass(format('public.%I', tabla)) IS NOT NULL THEN
            EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', tabla);
        END IF;
    END LOOP;
END $$;

-- 5) Revocar privilegios a roles públicos de Supabase si existen
DO $$
DECLARE
    rol    TEXT;
    tabla  TEXT;
    roles  TEXT[] := ARRAY['anon', 'authenticated'];
    tablas TEXT[] := ARRAY[
        'categoria_producto',
        'producto',
        'producto_variante',
        'producto_imagen'
    ];
BEGIN
    FOREACH rol IN ARRAY roles LOOP
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = rol) THEN
            FOREACH tabla IN ARRAY tablas LOOP
                IF to_regclass(format('public.%I', tabla)) IS NOT NULL THEN
                    EXECUTE format('REVOKE ALL ON TABLE public.%I FROM %I', tabla, rol);
                END IF;
            END LOOP;
        END IF;
    END LOOP;
END $$;
