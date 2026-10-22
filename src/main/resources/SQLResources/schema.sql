CREATE TABLE IF NOT EXISTS usuarios (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    nombre        VARCHAR(100) NOT NULL DEFAULT '',
    email         VARCHAR(254),
    password_hash VARCHAR(100) NOT NULL,
    rol           VARCHAR(20)  NOT NULL,
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

-- Este schema es temporal, despues hay que ordenar las cosas bien