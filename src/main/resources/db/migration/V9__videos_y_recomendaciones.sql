-- ============================================================================
-- V9: Videos de productos y recomendaciones de Sheis (Cloudflare R2 / YouTube)
-- ============================================================================

-- 1) Tabla de video por producto (1 a 1 opcional)
CREATE TABLE IF NOT EXISTS producto_video (
    id           BIGSERIAL PRIMARY KEY,
    producto_id  BIGINT NOT NULL UNIQUE REFERENCES producto(id) ON DELETE CASCADE,
    object_key   VARCHAR(255) NOT NULL,
    poster_key   VARCHAR(255) NOT NULL,
    duracion_seg INT NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    creado_en    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2) Tabla de recomendaciones y tutoriales de Sheis
CREATE TABLE IF NOT EXISTS recomendacion (
    id             BIGSERIAL PRIMARY KEY,
    titulo         VARCHAR(80) NOT NULL,
    descripcion    VARCHAR(140),
    tipo           VARCHAR(20) NOT NULL, -- 'CLIP' o 'YOUTUBE'
    object_key     VARCHAR(255),
    poster_key     VARCHAR(255),
    youtube_id     VARCHAR(30),
    duracion_seg   INT,
    tamano_bytes   BIGINT,
    producto_id    BIGINT REFERENCES producto(id) ON DELETE SET NULL,
    categoria_slug VARCHAR(60),
    orden          INT NOT NULL DEFAULT 1,
    activa         BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_recomendacion_tipo CHECK (
        (tipo = 'CLIP' AND object_key IS NOT NULL AND poster_key IS NOT NULL) OR
        (tipo = 'YOUTUBE' AND youtube_id IS NOT NULL)
    )
);

-- 3) Índices para acelerar consultas públicas y relaciones
CREATE INDEX IF NOT EXISTS idx_producto_video_producto ON producto_video(producto_id);
CREATE INDEX IF NOT EXISTS idx_recomendacion_activa_orden ON recomendacion(activa, orden);
CREATE INDEX IF NOT EXISTS idx_recomendacion_producto ON recomendacion(producto_id);
CREATE INDEX IF NOT EXISTS idx_recomendacion_categoria ON recomendacion(categoria_slug);

-- 4) Activar Row Level Security (RLS) como en V5 y V7
DO $$
DECLARE
    tabla  TEXT;
    tablas TEXT[] := ARRAY[
        'producto_video',
        'recomendacion'
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
        'producto_video',
        'recomendacion'
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
