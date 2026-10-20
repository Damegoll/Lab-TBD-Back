CREATE TABLE IF NOT EXISTS usuarios (
    id            BIGSERIAL    PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    rol           VARCHAR(20)  NOT NULL CHECK (rol IN ('ADMIN', 'USER'))
);

CREATE TABLE IF NOT EXISTS tareas (
    id          BIGSERIAL     PRIMARY KEY,
    titulo      VARCHAR(150)  NOT NULL,
    descripcion VARCHAR(1000),
    completada  BOOLEAN       NOT NULL DEFAULT FALSE,
    creada_en   TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Este schema es temporal, despues hay que ordenar las cosas bien