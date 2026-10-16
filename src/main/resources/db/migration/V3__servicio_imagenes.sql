-- V3: Almacenamiento de imágenes/logos por servicio en base de datos (PostgreSQL bytea)
CREATE TABLE servicio_imagenes (
  servicio_id     BIGINT PRIMARY KEY REFERENCES sku_categorias(id) ON DELETE CASCADE,
  content_type    TEXT NOT NULL,
  datos           BYTEA NOT NULL,
  bytes_len       INTEGER NOT NULL,
  actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);
