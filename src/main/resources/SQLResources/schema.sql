CREATE TABLE IF NOT EXISTS usuarios (
                                        id            BIGSERIAL PRIMARY KEY,
                                        username      VARCHAR(50)  NOT NULL UNIQUE,
    nombre        VARCHAR(100) NOT NULL DEFAULT '',
    email         VARCHAR(254),
    password_hash VARCHAR(100) NOT NULL,
    rol           VARCHAR(20)  NOT NULL CHECK (rol IN ('USUARIO', 'OPERADOR', 'ADMIN')),
    activo        BOOLEAN      NOT NULL DEFAULT TRUE
    );

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS nombre VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS email VARCHAR(254);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_rol_check;

UPDATE usuarios
SET email = username || '@localhost'
WHERE email IS NULL OR email = '';

UPDATE usuarios SET rol = 'USUARIO' WHERE rol = 'USER';

ALTER TABLE usuarios ALTER COLUMN email SET NOT NULL;
ALTER TABLE usuarios ADD CONSTRAINT usuarios_rol_check
    CHECK (rol IN ('USUARIO', 'OPERADOR', 'ADMIN'));
CREATE UNIQUE INDEX IF NOT EXISTS usuarios_email_key ON usuarios(email);

CREATE TABLE IF NOT EXISTS tareas (
                                      id          BIGSERIAL     PRIMARY KEY,
                                      titulo      VARCHAR(150)  NOT NULL,
    descripcion VARCHAR(1000),
    completada  BOOLEAN       NOT NULL DEFAULT FALSE,
    creada_en   TIMESTAMPTZ   NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS estaciones (
                                          id      BIGSERIAL PRIMARY KEY,
                                          nombre  VARCHAR(100) NOT NULL,
    activa  BOOLEAN NOT NULL DEFAULT TRUE
    );

CREATE TABLE IF NOT EXISTS vehiculos (
                                         id           BIGSERIAL PRIMARY KEY,
                                         codigo       VARCHAR(50) NOT NULL UNIQUE,
    tipo         VARCHAR(50) NOT NULL CHECK (tipo IN ('BICICLETA', 'SCOOTER')),
    estado       VARCHAR(50) NOT NULL CHECK (estado IN ('DISPONIBLE', 'EN_USO', 'EN_MANTENCION', 'DADO_DE_BAJA')),
    bateria      INT NOT NULL CHECK (bateria BETWEEN 0 AND 100),
    estacion_id  BIGINT REFERENCES estaciones(id) ON DELETE SET NULL
    );

CREATE TABLE IF NOT EXISTS auditoria_estado_vehiculo (
                                                         id              BIGSERIAL PRIMARY KEY,
                                                         vehiculo_id     BIGINT NOT NULL REFERENCES vehiculos(id) ON DELETE CASCADE,
    estado_anterior VARCHAR(50),
    estado_nuevo    VARCHAR(50) NOT NULL,
    operador        VARCHAR(100),
    fecha           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    motivo          TEXT
    );

CREATE TABLE IF NOT EXISTS tarifas (
                                       id                 BIGSERIAL PRIMARY KEY,
                                       tipo_vehiculo      VARCHAR(50) NOT NULL,
    precio_por_minuto  NUMERIC(10, 2) NOT NULL,
    vigente_desde      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    vigente_hasta      TIMESTAMPTZ,
    activa             BOOLEAN NOT NULL DEFAULT TRUE
    );

CREATE TABLE IF NOT EXISTS billeteras (
                                          id          BIGSERIAL PRIMARY KEY,
                                          usuario_id  BIGINT         NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    saldo       NUMERIC(10, 2) NOT NULL DEFAULT 0.00
    );

CREATE TABLE IF NOT EXISTS viajes (
                                      id             BIGSERIAL PRIMARY KEY,
                                      usuario_id     BIGINT      NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    vehiculo_id    BIGINT      NOT NULL REFERENCES vehiculos(id) ON DELETE CASCADE,
    iniciado_en    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finalizado_en  TIMESTAMPTZ,
    costo          NUMERIC(10, 2)
    );

CREATE TABLE IF NOT EXISTS ordenes_traslado (
                                                id                  BIGSERIAL PRIMARY KEY,
                                                solicitud_id        UUID         NOT NULL,
                                                estacion_origen_id  BIGINT       NOT NULL REFERENCES estaciones(id),
    estacion_destino_id BIGINT       NOT NULL REFERENCES estaciones(id),
    tipo_vehiculo       VARCHAR(50)  NOT NULL,
    cantidad            INT          NOT NULL CHECK (cantidad > 0),
    operador_id         BIGINT       NOT NULL REFERENCES usuarios(id),
    creada_en           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_vehiculos_estado ON vehiculos (estado);
CREATE INDEX IF NOT EXISTS idx_viajes_usuario_id ON viajes (usuario_id);
CREATE INDEX IF NOT EXISTS idx_viajes_iniciado_en ON viajes (iniciado_en);