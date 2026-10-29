-- ==========================================
-- 1. VISTA MATERIALIZADA Y SUS ÍNDICES
-- ==========================================
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_demanda_estacion_horaria AS
SELECT
    e.id AS estacion_id,
    e.nombre AS estacion_nombre,
    EXTRACT(HOUR FROM t.fecha)::int AS hora_dia,
    SUM(t.iniciado) AS viajes_iniciados,
    SUM(t.finalizado) AS viajes_finalizados
FROM (
         SELECT
             v.estacion_id AS estacion_id,
             j.iniciado_en AS fecha,
             1 AS iniciado,
             0 AS finalizado
         FROM viajes j
                  JOIN vehiculos v ON j.vehiculo_id = v.id
         WHERE j.iniciado_en IS NOT NULL

         UNION ALL

         SELECT
             v.estacion_id AS estacion_id,
             j.finalizado_en AS fecha,
             0 AS iniciado,
             1 AS finalizado
         FROM viajes j
                  JOIN vehiculos v ON j.vehiculo_id = v.id
         WHERE j.finalizado_en IS NOT NULL
     ) t
         JOIN estaciones e ON t.estacion_id = e.id
GROUP BY e.id, e.nombre, EXTRACT(HOUR FROM t.fecha);

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_demanda_estacion_hora
    ON mv_demanda_estacion_horaria (estacion_id, hora_dia);

-- ==========================================
-- 2. FUNCIONES Y TRIGGERS
-- ==========================================
CREATE OR REPLACE FUNCTION fn_validar_inicio_viaje()
RETURNS TRIGGER AS $$
DECLARE
v_saldo NUMERIC(10,2);
    v_bateria INT;
    v_viajes_activos INT;
BEGIN
SELECT saldo INTO v_saldo FROM billeteras WHERE usuario_id = NEW.usuario_id;
IF v_saldo IS NOT NULL AND v_saldo < 0 THEN
        RAISE EXCEPTION 'No se puede iniciar el viaje: El usuario tiene saldo negativo (Saldo: %)', v_saldo;
END IF;

SELECT COUNT(*) INTO v_viajes_activos FROM viajes WHERE usuario_id = NEW.usuario_id AND finalizado_en IS NULL;
IF v_viajes_activos > 0 THEN
        RAISE EXCEPTION 'No se puede iniciar el viaje: El usuario ya posee un viaje activo en curso.';
END IF;

SELECT bateria INTO v_bateria FROM vehiculos WHERE id = NEW.vehiculo_id;
IF v_bateria IS NOT NULL AND v_bateria < 20 THEN
        RAISE EXCEPTION 'No se puede iniciar el viaje: La batería del vehículo es inferior al 20%% (Batería: %)', v_bateria;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validar_inicio_viaje ON viajes;
CREATE TRIGGER trg_validar_inicio_viaje
    BEFORE INSERT ON viajes
    FOR EACH ROW
    EXECUTE FUNCTION fn_validar_inicio_viaje();


CREATE OR REPLACE FUNCTION fn_auditar_estado_vehiculo()
RETURNS TRIGGER AS $$
DECLARE
v_operador BIGINT;
    v_motivo TEXT;
    v_setting_op TEXT;
BEGIN
    IF NEW.estado IN ('EN_MANTENCION', 'DADO_DE_BAJA') AND (OLD.estado IS DISTINCT FROM NEW.estado) THEN
        v_setting_op := current_setting('app.operador', true);
        IF v_setting_op ~ '^[0-9]+$' THEN
            v_operador := v_setting_op::bigint;
ELSE
            v_operador := NULL;
END IF;

        v_motivo := current_setting('app.motivo', true);
        IF v_motivo IS NULL OR v_motivo = '' THEN
            v_motivo := 'Cambio de estado a ' || NEW.estado;
END IF;

INSERT INTO auditoria_estado_vehiculo (vehiculo_id, estado_anterior, estado_nuevo, operador, fecha, motivo)
VALUES (NEW.id, OLD.estado, NEW.estado, v_operador::text, NOW(), v_motivo);
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_estado_vehiculo ON vehiculos;
CREATE TRIGGER trg_auditar_estado_vehiculo
    AFTER UPDATE ON vehiculos
    FOR EACH ROW
    EXECUTE FUNCTION fn_auditar_estado_vehiculo();

-- ==========================================
-- 3. PROCEDIMIENTOS ALMACENADOS
-- ==========================================
CREATE OR REPLACE PROCEDURE finalizar_viaje(
    p_viaje_id BIGINT,
    p_estacion_destino_id BIGINT
)
LANGUAGE plpgsql
AS $$
DECLARE
v_usuario_id BIGINT;
    v_vehiculo_id BIGINT;
    v_tipo_vehiculo VARCHAR(50);
    v_iniciado_en TIMESTAMPTZ;
    v_minutos NUMERIC;
    v_precio_minuto NUMERIC;
    v_costo_total NUMERIC;
    v_saldo_actual NUMERIC;
    v_estado_anterior VARCHAR(50);
BEGIN
SELECT v.usuario_id, v.vehiculo_id, v.iniciado_en, veh.tipo, veh.estado
INTO v_usuario_id, v_vehiculo_id, v_iniciado_en, v_tipo_vehiculo, v_estado_anterior
FROM viajes v
         JOIN vehiculos veh ON v.vehiculo_id = veh.id
WHERE v.id = p_viaje_id AND v.finalizado_en IS NULL;

IF NOT FOUND THEN
        RAISE EXCEPTION 'El viaje con ID % no existe o ya se encuentra finalizado.', p_viaje_id;
END IF;

    v_minutos := GREATEST(CEIL(EXTRACT(EPOCH FROM (NOW() - v_iniciado_en)) / 60.0), 1);

SELECT precio_por_minuto INTO v_precio_minuto
FROM tarifas
WHERE tipo_vehiculo = v_tipo_vehiculo AND activa = TRUE
  AND vigente_desde <= NOW() AND (vigente_hasta IS NULL OR vigente_hasta >= NOW())
    LIMIT 1;

IF v_precio_minuto IS NULL THEN
        RAISE EXCEPTION 'No se encontró una tarifa activa para el tipo de vehículo %', v_tipo_vehiculo;
END IF;

    v_costo_total := v_minutos * v_precio_minuto;

SELECT saldo INTO v_saldo_actual
FROM billeteras
WHERE usuario_id = v_usuario_id;

IF v_saldo_actual IS NULL THEN
        RAISE EXCEPTION 'El usuario con ID % no posee una billetera asociada.', v_usuario_id;
END IF;

    IF v_saldo_actual < v_costo_total THEN
        RAISE EXCEPTION 'Saldo insuficiente en la billetera. Saldo actual: %, Costo del viaje: %', v_saldo_actual, v_costo_total;
END IF;

UPDATE billeteras
SET saldo = saldo - v_costo_total
WHERE usuario_id = v_usuario_id;

UPDATE vehiculos
SET estacion_id = p_estacion_destino_id,
    estado = 'DISPONIBLE'
WHERE id = v_vehiculo_id;

INSERT INTO auditoria_estado_vehiculo (vehiculo_id, estado_anterior, estado_nuevo, operador, motivo)
VALUES (v_vehiculo_id, v_estado_anterior, 'DISPONIBLE', 'SISTEMA', 'Finalización de viaje');

UPDATE viajes
SET finalizado_en = NOW(),
    costo = v_costo_total
WHERE id = p_viaje_id;
END;
$$;


CREATE OR REPLACE PROCEDURE rebalancear_flota(
    p_estacion_origen_id BIGINT,
    p_estacion_destino_id BIGINT,
    p_tipo_vehiculo VARCHAR(50),
    p_cantidad INT,
    p_operador_id BIGINT,
    p_solicitud_id UUID
)
LANGUAGE plpgsql
AS $$
DECLARE
v_vehiculos_disponibles INT;
BEGIN
    IF p_estacion_origen_id = p_estacion_destino_id THEN
        RAISE EXCEPTION 'La estación de origen y destino no pueden ser la misma.';
END IF;

SELECT COUNT(*) INTO v_vehiculos_disponibles
FROM vehiculos
WHERE estacion_id = p_estacion_origen_id
  AND tipo = p_tipo_vehiculo
  AND estado = 'DISPONIBLE';

IF v_vehiculos_disponibles < p_cantidad THEN
        RAISE EXCEPTION 'Stock insuficiente en origen. Disponibles: %, Solicitados: %', v_vehiculos_disponibles, p_cantidad;
END IF;

UPDATE vehiculos
SET estacion_id = p_estacion_destino_id
WHERE id IN (
    SELECT id
    FROM vehiculos
    WHERE estacion_id = p_estacion_origen_id
      AND tipo = p_tipo_vehiculo
      AND estado = 'DISPONIBLE'
    LIMIT p_cantidad
    );

INSERT INTO ordenes_traslado (
    solicitud_id,
    estacion_origen_id,
    estacion_destino_id,
    tipo_vehiculo,
    cantidad,
    operador_id,
    creada_en
) VALUES (
             COALESCE(p_solicitud_id, gen_random_uuid()),
             p_estacion_origen_id,
             p_estacion_destino_id,
             p_tipo_vehiculo,
             p_cantidad,
             p_operador_id,
             NOW()
         );
END;
$$;