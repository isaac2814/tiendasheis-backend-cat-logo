-- V2: tabla de usuarios administradores para autenticación del panel
CREATE TABLE admin_usuarios (
  id            BIGSERIAL PRIMARY KEY,
  email         TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  nombre        TEXT,
  activo        BOOLEAN DEFAULT TRUE,
  creado_en     TIMESTAMPTZ DEFAULT NOW()
);
