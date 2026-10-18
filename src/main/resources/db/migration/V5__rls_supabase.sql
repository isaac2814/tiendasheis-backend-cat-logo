-- ============================================================================
-- V5: Row Level Security (RLS) para Supabase.
-- ============================================================================
-- La base de producción es un proyecto de Supabase, que expone las tablas del
-- esquema `public` por su API REST (PostgREST) con una clave pública (`anon`).
-- El backend NO usa esa API: se conecta directo por JDBC como dueño de las tablas.
--
-- Esta migración activa RLS en TODAS las tablas de la aplicación SIN crear
-- políticas. Resultado: un rol sin BYPASSRLS (p. ej. `anon`) no puede leer ni
-- escribir ninguna fila. El dueño de las tablas (usuario JDBC) sí sigue operando
-- con normalidad, porque en PostgreSQL el dueño de la tabla está exento de RLS
-- salvo que se active FORCE ROW LEVEL SECURITY (que aquí NO se activa).
--
-- Seguridad para bases locales de pruebas:
--   - No falla si un rol (`anon`/`authenticated`) no existe.
--   - No falla si una tabla no existe (se comprueba con to_regclass).
-- ============================================================================

-- 1) Activar RLS en todas las tablas de la aplicación (incluida la de Flyway).
DO $$
DECLARE
    tabla  TEXT;
    tablas TEXT[] := ARRAY[
        'servicios',
        'sku_categorias',
        'tarifas',
        'descuentos_combo',
        'combos',
        'combo_items',
        'metodos_pago',
        'horarios',
        'catalogo_config',
        'admin_usuarios',
        'servicio_imagenes',
        'metodo_pago_imagenes',
        'flyway_schema_history'
    ];
BEGIN
    FOREACH tabla IN ARRAY tablas LOOP
        IF to_regclass(format('public.%I', tabla)) IS NOT NULL THEN
            EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', tabla);
        END IF;
    END LOOP;
END $$;

-- 2) Revocar privilegios a los roles públicos de Supabase, SOLO si existen.
--    (En una base local de pruebas que no sea Supabase estos roles no existen,
--    por lo que el bloque no hace nada.)
DO $$
DECLARE
    rol    TEXT;
    tabla  TEXT;
    roles  TEXT[] := ARRAY['anon', 'authenticated'];
    tablas TEXT[] := ARRAY[
        'servicios',
        'sku_categorias',
        'tarifas',
        'descuentos_combo',
        'combos',
        'combo_items',
        'metodos_pago',
        'horarios',
        'catalogo_config',
        'admin_usuarios',
        'servicio_imagenes',
        'metodo_pago_imagenes',
        'flyway_schema_history'
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
