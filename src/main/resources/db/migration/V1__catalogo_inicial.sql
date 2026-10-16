-- V1: esquema inicial del catálogo TiendaSheis (base de pruebas).
-- Nombres alineados con tu base real (servicios, sku_categorias, tarifas, descuentos_combo)
-- para poder migrar después. El stock es MANUAL solo en esta base de pruebas: en la real
-- se calcula desde recursos DISPONIBLE de cuentas ACTIVA (ver interfaz StockProvider).

CREATE TABLE servicios (
  id          BIGSERIAL PRIMARY KEY,
  nombre      TEXT NOT NULL UNIQUE
);

CREATE TABLE sku_categorias (
  id              BIGSERIAL PRIMARY KEY,
  servicio_id     BIGINT NOT NULL REFERENCES servicios(id),
  codigo          TEXT NOT NULL UNIQUE,            -- 'nc', 'ne', ... (id estable para el front)
  nombre          TEXT NOT NULL,                   -- nombre mostrado en el catálogo
  modalidad       TEXT NOT NULL,
  inicial         VARCHAR(2) NOT NULL,
  color           VARCHAR(7) NOT NULL,             -- #RRGGBB de la ficha 3D
  logo_url        TEXT,                            -- logo opcional (256x256)
  etiqueta        TEXT NOT NULL DEFAULT '',        -- chip corto: 'Perfil compartido'
  descripcion     TEXT NOT NULL DEFAULT '',
  incluye         TEXT[] NOT NULL DEFAULT '{}',
  recomendaciones TEXT[] NOT NULL DEFAULT '{}',
  visible         BOOLEAN NOT NULL DEFAULT TRUE,
  orden           INT NOT NULL DEFAULT 0,
  stock_manual    INT,                             -- NULL = disponible, N = quedan N, 0 = agotado
  CONSTRAINT ck_color CHECK (color ~ '^#[0-9A-Fa-f]{6}$'),
  CONSTRAINT ck_stock CHECK (stock_manual IS NULL OR stock_manual >= 0)
);

CREATE TABLE tarifas (
  id                BIGSERIAL PRIMARY KEY,
  sku_categoria_id  BIGINT NOT NULL REFERENCES sku_categorias(id) ON DELETE CASCADE,
  dias              INT NOT NULL CHECK (dias > 0),
  cantidad_perfiles INT NOT NULL CHECK (cantidad_perfiles > 0),
  precio            NUMERIC(12,2) NOT NULL CHECK (precio >= 0),
  precio_distribuidor NUMERIC(12,2),
  activo            BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uq_tarifa UNIQUE (sku_categoria_id, dias, cantidad_perfiles)
);

CREATE TABLE descuentos_combo (
  cantidad_servicios INT PRIMARY KEY CHECK (cantidad_servicios >= 2),
  descuento          NUMERIC(12,2) NOT NULL CHECK (descuento >= 0),
  activo             BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE combos (
  id       BIGSERIAL PRIMARY KEY,
  etiqueta TEXT NOT NULL DEFAULT '',   -- 'Popular', 'El completo'...
  activo   BOOLEAN NOT NULL DEFAULT TRUE,
  orden    INT NOT NULL DEFAULT 0
);

CREATE TABLE combo_items (
  combo_id         BIGINT NOT NULL REFERENCES combos(id) ON DELETE CASCADE,
  sku_categoria_id BIGINT NOT NULL REFERENCES sku_categorias(id) ON DELETE CASCADE,
  PRIMARY KEY (combo_id, sku_categoria_id)
);

CREATE TABLE metodos_pago (
  id           TEXT PRIMARY KEY,        -- 'nequi', 'breb'
  nombre       TEXT NOT NULL,
  inicial      VARCHAR(2) NOT NULL,
  color        VARCHAR(7) NOT NULL,
  etiqueta_dato TEXT NOT NULL,          -- 'Número de Nequi' / 'Llave Bre-b'
  dato         TEXT NOT NULL,
  titular      TEXT NOT NULL,
  activo       BOOLEAN NOT NULL DEFAULT TRUE,
  orden        INT NOT NULL DEFAULT 0
);

CREATE TABLE horarios (
  orden INT PRIMARY KEY,
  dia   TEXT NOT NULL,
  texto TEXT NOT NULL
);

-- Clave/valor para ajustes sueltos (whatsapp, sobre_nosotros, descuento_extra_desde_6)
CREATE TABLE catalogo_config (
  clave TEXT PRIMARY KEY,
  valor TEXT NOT NULL
);

CREATE INDEX ix_tarifas_categoria ON tarifas(sku_categoria_id);
CREATE INDEX ix_categorias_visible ON sku_categorias(visible, orden);

-- ===================== DATOS DE EJEMPLO =====================

INSERT INTO servicios (nombre) VALUES
  ('Netflix'),
  ('Disney+'),
  ('HBO Max'),
  ('Amazon Prime'),
  ('Crunchyroll'),
  ('Paramount+'),
  ('ViX'),
  ('Canva'),
  ('Spotify');

INSERT INTO sku_categorias (servicio_id, codigo, nombre, modalidad, inicial, color, etiqueta, descripcion, incluye, recomendaciones, visible, orden, stock_manual) VALUES
  ((SELECT id FROM servicios WHERE nombre='Netflix'), 'nc', 'Netflix Compartido', 'Compartido', 'N', '#8B1A1A', 'Perfil compartido', 'Netflix reúne series, películas, documentales y contenido original. Con este plan recibes un perfil propio dentro de una cuenta que se comparte con otros clientes.', ARRAY['Perfil con PIN propio','Soporte por WhatsApp','Entrega inmediata al completar tu pago'], ARRAY['Usa solo el perfil que te asignamos y no cambies su nombre ni su PIN.','No cambies el correo ni la contraseña de la cuenta.','Si algo falla, escríbenos por WhatsApp con tu código de cliente.','Escríbenos antes de tu fecha de vencimiento si quieres renovar.','Es una cuenta compartida: respeta los perfiles de los demás usuarios.'], TRUE, 1, NULL),
  ((SELECT id FROM servicios WHERE nombre='Netflix'), 'ne', 'Netflix Exclusivo', 'Exclusivo', 'N', '#5C1010', 'Plan exclusivo', 'Netflix reúne series, películas, documentales y contenido original. Este es el plan exclusivo.', ARRAY['Perfil con PIN propio','Soporte por WhatsApp','Entrega inmediata al completar tu pago','[Explica aquí qué incluye el plan exclusivo]'], ARRAY['Usa solo el perfil que te asignamos y no cambies su nombre ni su PIN.','No cambies el correo ni la contraseña de la cuenta.','Si algo falla, escríbenos por WhatsApp con tu código de cliente.','Escríbenos antes de tu fecha de vencimiento si quieres renovar.'], TRUE, 2, NULL),
  ((SELECT id FROM servicios WHERE nombre='Disney+'), 'de', 'Disney Estándar', 'Estándar', 'D', '#0F4C6B', 'Plan estándar', 'Disney+ reúne contenido de Disney, Pixar, Marvel, Star Wars y National Geographic. Con este plan recibes un perfil propio.', ARRAY['Perfil con PIN propio','Soporte por WhatsApp','Entrega inmediata al completar tu pago'], ARRAY['Usa solo el perfil que te asignamos y no cambies su nombre ni su PIN.','No cambies el correo ni la contraseña de la cuenta.','Si algo falla, escríbenos por WhatsApp con tu código de cliente.','Escríbenos antes de tu fecha de vencimiento si quieres renovar.'], TRUE, 3, 2),
  ((SELECT id FROM servicios WHERE nombre='Disney+'), 'dp', 'Disney Premium', 'Premium', 'D', '#0A3550', 'Plan premium', 'Disney+ reúne contenido de Disney, Pixar, Marvel, Star Wars y National Geographic. Este es el plan premium, con la mejor calidad de imagen.', ARRAY['Perfil con PIN propio','Soporte por WhatsApp','Entrega inmediata al completar tu pago'], ARRAY['Usa solo el perfil que te asignamos y no cambies su nombre ni su PIN.','No cambies el correo ni la contraseña de la cuenta.','Si algo falla, escríbenos por WhatsApp con tu código de cliente.','Escríbenos antes de tu fecha de vencimiento si quieres renovar.'], TRUE, 4, 2),
  ((SELECT id FROM servicios WHERE nombre='HBO Max'), 'hb', 'HBO Max', 'Perfil', 'H', '#3A2A6B', 'Perfil propio', 'HBO Max reúne series y películas de HBO, Warner Bros. y DC, además de estrenos y contenido original. Recibes un perfil propio.', ARRAY['Perfil con PIN propio','Soporte por WhatsApp','Entrega inmediata al completar tu pago'], ARRAY['Usa solo el perfil que te asignamos y no cambies su nombre ni su PIN.','No cambies el correo ni la contraseña de la cuenta.','Si algo falla, escríbenos por WhatsApp con tu código de cliente.','Escríbenos antes de tu fecha de vencimiento si quieres renovar.'], TRUE, 5, NULL),
  ((SELECT id FROM servicios WHERE nombre='Amazon Prime'), 'pr', 'Amazon Prime', 'Perfil', 'P', '#1B5FA8', 'Perfil propio', 'Prime Video ofrece películas, series y originales de Amazon. Recibes un perfil propio.', ARRAY['Perfil con PIN propio','Soporte por WhatsApp','Entrega inmediata al completar tu pago'], ARRAY['Usa solo el perfil que te asignamos y no cambies su nombre ni su PIN.','No cambies el correo ni la contraseña de la cuenta.','Si algo falla, escríbenos por WhatsApp con tu código de cliente.','Escríbenos antes de tu fecha de vencimiento si quieres renovar.'], TRUE, 6, NULL),
  ((SELECT id FROM servicios WHERE nombre='Crunchyroll'), 'cr', 'Crunchyroll', 'Perfil', 'C', '#B8651B', 'Perfil propio', 'Crunchyroll es la plataforma del anime: un amplio catálogo de series y películas, con estrenos de temporada. Recibes un perfil propio.', ARRAY['Perfil con PIN propio','Soporte por WhatsApp','Entrega inmediata al completar tu pago'], ARRAY['Usa solo el perfil que te asignamos y no cambies su nombre ni su PIN.','No cambies el correo ni la contraseña de la cuenta.','Si algo falla, escríbenos por WhatsApp con tu código de cliente.','Escríbenos antes de tu fecha de vencimiento si quieres renovar.'], TRUE, 7, 0),
  ((SELECT id FROM servicios WHERE nombre='Paramount+'), 'pa', 'Paramount+', 'Perfil', 'P', '#1F4FBF', 'Perfil propio', 'Paramount+ reúne series, películas y contenido de Paramount y Nickelodeon. Recibes un perfil propio.', ARRAY['Perfil con PIN propio','Soporte por WhatsApp','Entrega inmediata al completar tu pago'], ARRAY['Usa solo el perfil que te asignamos y no cambies su nombre ni su PIN.','No cambies el correo ni la contraseña de la cuenta.','Si algo falla, escríbenos por WhatsApp con tu código de cliente.','Escríbenos antes de tu fecha de vencimiento si quieres renovar.'], TRUE, 8, NULL),
  ((SELECT id FROM servicios WHERE nombre='ViX'), 'vx', 'Vix', 'Perfil', 'V', '#C2410C', 'Perfil propio', 'ViX ofrece contenido en español: novelas, series, películas y canales en vivo. Recibes un perfil propio.', ARRAY['Perfil con PIN propio','Soporte por WhatsApp','Entrega inmediata al completar tu pago'], ARRAY['Usa solo el perfil que te asignamos y no cambies su nombre ni su PIN.','No cambies el correo ni la contraseña de la cuenta.','Si algo falla, escríbenos por WhatsApp con tu código de cliente.','Escríbenos antes de tu fecha de vencimiento si quieres renovar.'], TRUE, 9, 1),
  ((SELECT id FROM servicios WHERE nombre='Canva'), 'cv', 'Canva', 'Correo o invitación', 'C', '#0E7C86', 'Correo o invitación', 'Canva te permite diseñar presentaciones, publicaciones, logos y más con plantillas y herramientas de diseño. La entrega es con correo y contraseña o por invitación, según disponibilidad.', ARRAY['Acceso por correo y contraseña o por invitación','Soporte por WhatsApp','Entrega inmediata al completar tu pago'], ARRAY['No cambies el correo ni la contraseña de la cuenta.','Si el acceso es por invitación, acéptala desde el correo que nos indiques.','Si algo falla, escríbenos por WhatsApp con tu código de cliente.','Escríbenos antes de tu fecha de vencimiento si quieres renovar.'], TRUE, 10, 2),
  ((SELECT id FROM servicios WHERE nombre='Spotify'), 'sp', 'Spotify', 'Premium', 'S', '#1E6B3A', 'Música y podcasts', 'Spotify te da música y podcasts para escuchar donde quieras.', ARRAY['Soporte por WhatsApp','Entrega inmediata al completar tu pago'], ARRAY['No cambies el correo ni la contraseña de la cuenta.','Si algo falla, escríbenos por WhatsApp con tu código de cliente.','Escríbenos antes de tu fecha de vencimiento si quieres renovar.'], TRUE, 11, 0);

-- Netflix Compartido, Netflix Exclusivo, Disney Estándar y Disney Premium tienen tus precios reales.
-- Los demás servicios son precios de EJEMPLO: ajústalos desde el panel.
INSERT INTO tarifas (sku_categoria_id, dias, cantidad_perfiles, precio) VALUES
  ((SELECT id FROM sku_categorias WHERE codigo='nc'), 15, 1, 7900),
  ((SELECT id FROM sku_categorias WHERE codigo='nc'), 30, 1, 14400),
  ((SELECT id FROM sku_categorias WHERE codigo='nc'), 60, 1, 26300),
  ((SELECT id FROM sku_categorias WHERE codigo='nc'), 90, 1, 39000),
  ((SELECT id FROM sku_categorias WHERE codigo='nc'), 30, 2, 26300),
  ((SELECT id FROM sku_categorias WHERE codigo='nc'), 60, 2, 52100),
  ((SELECT id FROM sku_categorias WHERE codigo='ne'), 30, 1, 16900),
  ((SELECT id FROM sku_categorias WHERE codigo='de'), 30, 1, 7500),
  ((SELECT id FROM sku_categorias WHERE codigo='dp'), 30, 1, 10000),
  ((SELECT id FROM sku_categorias WHERE codigo='hb'), 30, 1, 6900),
  ((SELECT id FROM sku_categorias WHERE codigo='hb'), 30, 2, 13100),
  ((SELECT id FROM sku_categorias WHERE codigo='hb'), 30, 3, 18600),
  ((SELECT id FROM sku_categorias WHERE codigo='hb'), 60, 1, 13100),
  ((SELECT id FROM sku_categorias WHERE codigo='hb'), 60, 2, 24900),
  ((SELECT id FROM sku_categorias WHERE codigo='hb'), 60, 3, 35400),
  ((SELECT id FROM sku_categorias WHERE codigo='hb'), 90, 1, 18600),
  ((SELECT id FROM sku_categorias WHERE codigo='hb'), 90, 2, 35400),
  ((SELECT id FROM sku_categorias WHERE codigo='hb'), 90, 3, 50300),
  ((SELECT id FROM sku_categorias WHERE codigo='pr'), 30, 1, 6900),
  ((SELECT id FROM sku_categorias WHERE codigo='pr'), 30, 2, 13100),
  ((SELECT id FROM sku_categorias WHERE codigo='pr'), 30, 3, 18600),
  ((SELECT id FROM sku_categorias WHERE codigo='pr'), 60, 1, 13100),
  ((SELECT id FROM sku_categorias WHERE codigo='pr'), 60, 2, 24900),
  ((SELECT id FROM sku_categorias WHERE codigo='pr'), 60, 3, 35400),
  ((SELECT id FROM sku_categorias WHERE codigo='pr'), 90, 1, 18600),
  ((SELECT id FROM sku_categorias WHERE codigo='pr'), 90, 2, 35400),
  ((SELECT id FROM sku_categorias WHERE codigo='pr'), 90, 3, 50300),
  ((SELECT id FROM sku_categorias WHERE codigo='cr'), 30, 1, 5900),
  ((SELECT id FROM sku_categorias WHERE codigo='cr'), 60, 1, 11200),
  ((SELECT id FROM sku_categorias WHERE codigo='cr'), 90, 1, 15900),
  ((SELECT id FROM sku_categorias WHERE codigo='pa'), 30, 1, 9000),
  ((SELECT id FROM sku_categorias WHERE codigo='pa'), 30, 2, 17100),
  ((SELECT id FROM sku_categorias WHERE codigo='pa'), 60, 1, 17100),
  ((SELECT id FROM sku_categorias WHERE codigo='pa'), 60, 2, 32500),
  ((SELECT id FROM sku_categorias WHERE codigo='pa'), 90, 1, 24300),
  ((SELECT id FROM sku_categorias WHERE codigo='pa'), 90, 2, 46200),
  ((SELECT id FROM sku_categorias WHERE codigo='vx'), 30, 1, 5900),
  ((SELECT id FROM sku_categorias WHERE codigo='vx'), 60, 1, 11200),
  ((SELECT id FROM sku_categorias WHERE codigo='vx'), 90, 1, 15900),
  ((SELECT id FROM sku_categorias WHERE codigo='cv'), 30, 1, 8000),
  ((SELECT id FROM sku_categorias WHERE codigo='cv'), 60, 1, 15200),
  ((SELECT id FROM sku_categorias WHERE codigo='cv'), 90, 1, 21600),
  ((SELECT id FROM sku_categorias WHERE codigo='sp'), 30, 1, 9900),
  ((SELECT id FROM sku_categorias WHERE codigo='sp'), 60, 1, 18800),
  ((SELECT id FROM sku_categorias WHERE codigo='sp'), 90, 1, 26700);

INSERT INTO descuentos_combo (cantidad_servicios, descuento) VALUES (2,2500),(3,5000),(4,7000),(5,9000);

INSERT INTO combos (etiqueta, activo, orden) VALUES
  ('El completo', TRUE, 1),
  ('', TRUE, 2),
  ('Popular', TRUE, 3),
  ('', TRUE, 4),
  ('', TRUE, 5),
  ('', TRUE, 6),
  ('El más pedido', TRUE, 7),
  ('', TRUE, 8);

INSERT INTO combo_items (combo_id, sku_categoria_id) VALUES
  ((SELECT id FROM combos WHERE orden=1), (SELECT id FROM sku_categorias WHERE codigo='nc')),
  ((SELECT id FROM combos WHERE orden=1), (SELECT id FROM sku_categorias WHERE codigo='de')),
  ((SELECT id FROM combos WHERE orden=1), (SELECT id FROM sku_categorias WHERE codigo='hb')),
  ((SELECT id FROM combos WHERE orden=1), (SELECT id FROM sku_categorias WHERE codigo='pr')),
  ((SELECT id FROM combos WHERE orden=2), (SELECT id FROM sku_categorias WHERE codigo='ne')),
  ((SELECT id FROM combos WHERE orden=2), (SELECT id FROM sku_categorias WHERE codigo='de')),
  ((SELECT id FROM combos WHERE orden=2), (SELECT id FROM sku_categorias WHERE codigo='hb')),
  ((SELECT id FROM combos WHERE orden=2), (SELECT id FROM sku_categorias WHERE codigo='pr')),
  ((SELECT id FROM combos WHERE orden=3), (SELECT id FROM sku_categorias WHERE codigo='nc')),
  ((SELECT id FROM combos WHERE orden=3), (SELECT id FROM sku_categorias WHERE codigo='de')),
  ((SELECT id FROM combos WHERE orden=3), (SELECT id FROM sku_categorias WHERE codigo='hb')),
  ((SELECT id FROM combos WHERE orden=4), (SELECT id FROM sku_categorias WHERE codigo='nc')),
  ((SELECT id FROM combos WHERE orden=4), (SELECT id FROM sku_categorias WHERE codigo='de')),
  ((SELECT id FROM combos WHERE orden=4), (SELECT id FROM sku_categorias WHERE codigo='pr')),
  ((SELECT id FROM combos WHERE orden=5), (SELECT id FROM sku_categorias WHERE codigo='nc')),
  ((SELECT id FROM combos WHERE orden=5), (SELECT id FROM sku_categorias WHERE codigo='hb')),
  ((SELECT id FROM combos WHERE orden=5), (SELECT id FROM sku_categorias WHERE codigo='pa')),
  ((SELECT id FROM combos WHERE orden=6), (SELECT id FROM sku_categorias WHERE codigo='ne')),
  ((SELECT id FROM combos WHERE orden=6), (SELECT id FROM sku_categorias WHERE codigo='de')),
  ((SELECT id FROM combos WHERE orden=6), (SELECT id FROM sku_categorias WHERE codigo='hb')),
  ((SELECT id FROM combos WHERE orden=7), (SELECT id FROM sku_categorias WHERE codigo='nc')),
  ((SELECT id FROM combos WHERE orden=7), (SELECT id FROM sku_categorias WHERE codigo='de')),
  ((SELECT id FROM combos WHERE orden=8), (SELECT id FROM sku_categorias WHERE codigo='nc')),
  ((SELECT id FROM combos WHERE orden=8), (SELECT id FROM sku_categorias WHERE codigo='hb'));

INSERT INTO metodos_pago (id, nombre, inicial, color, etiqueta_dato, dato, titular, activo, orden) VALUES
  ('nequi', 'Nequi', 'N', '#6A2C91', 'Número de Nequi', '3167846813', '[NOMBRE DEL TITULAR]', TRUE, 1),
  ('breb', 'Llaves (Bre-b)', 'B', '#0F6B5C', 'Llave Bre-b', '[TU LLAVE BRE-B]', '[NOMBRE DEL TITULAR]', TRUE, 2);

INSERT INTO horarios (orden, dia, texto) VALUES
  (1, 'Lunes a viernes', '8:00 AM - 10:00 PM'),
  (2, 'Sábados', '8:00 AM - 4:30 PM'),
  (3, 'Domingos', 'Intermitentes');

INSERT INTO catalogo_config (clave, valor) VALUES
  ('whatsapp', '57 300 000 0000'),
  ('descuento_extra_desde_6', '2000'),
  ('sobre_nosotros', 'Ofrecemos acceso a tus plataformas de streaming favoritas a precios justos, en servicios individuales o en combos para ahorrar más. Con más de 2 años de experiencia y brindando un buen servicio a nuestros usuarios.');
