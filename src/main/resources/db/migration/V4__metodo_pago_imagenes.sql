-- V4: Almacenamiento de imágenes de métodos de pago en base de datos (PostgreSQL bytea)
CREATE TABLE metodo_pago_imagenes (
  metodo_pago_id  TEXT PRIMARY KEY REFERENCES metodos_pago(id) ON DELETE CASCADE,
  content_type    TEXT NOT NULL,
  datos           BYTEA NOT NULL,
  bytes_len       INTEGER NOT NULL,
  actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);
