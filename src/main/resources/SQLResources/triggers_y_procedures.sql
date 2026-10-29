DROP MATERIALIZED VIEW IF EXISTS mv_demanda_estacion_horaria;

CREATE MATERIALIZED VIEW mv_demanda_estacion_horaria AS
SELECT
    e.id AS estacion_id,
    e.nombre AS estacion_nombre,
    EXTRACT(HOUR FROM eventos.fecha)::int AS hora_dia,
    SUM(eventos.iniciado) AS viajes_iniciados,
    SUM(eventos.finalizado) AS viajes_finalizados
FROM (
    SELECT estacion_origen_id AS estacion_id, iniciado_en AS fecha, 1 AS iniciado, 0 AS finalizado
    FROM viajes
    UNION ALL
    SELECT estacion_destino_id AS estacion_id, finalizado_en AS fecha, 0 AS iniciado, 1 AS finalizado
    FROM viajes
    WHERE finalizado_en IS NOT NULL
) eventos
JOIN estaciones e ON e.id = eventos.estacion_id
GROUP BY e.id, e.nombre, EXTRACT(HOUR FROM eventos.fecha);

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_demanda_estacion_hora
    ON mv_demanda_estacion_horaria (estacion_id, hora_dia);

CREATE OR REPLACE FUNCTION fn_crear_billetera_usuario()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO billeteras (usuario_id) VALUES (NEW.id)
    ON CONFLICT (usuario_id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_crear_billetera_usuario ON usuarios;
CREATE TRIGGER trg_crear_billetera_usuario
    AFTER INSERT ON usuarios
    FOR EACH ROW
    EXECUTE FUNCTION fn_crear_billetera_usuario();

CREATE OR REPLACE FUNCTION fn_validar_inicio_viaje()
RETURNS TRIGGER AS $$
DECLARE
    v_saldo NUMERIC(10, 2);
    v_bateria INT;
    v_estado VARCHAR(50);
    v_estacion_id BIGINT;
BEGIN
    SELECT saldo
    INTO v_saldo
    FROM billeteras
    WHERE usuario_id = NEW.usuario_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El usuario no tiene una billetera asociada';
    END IF;
    IF v_saldo < 0 THEN
        RAISE EXCEPTION 'No se puede iniciar el viaje: el saldo es negativo';
    END IF;
    IF EXISTS (
        SELECT 1 FROM viajes
        WHERE usuario_id = NEW.usuario_id AND finalizado_en IS NULL
    ) THEN
        RAISE EXCEPTION 'El usuario ya tiene un viaje activo';
    END IF;

    SELECT bateria, estado, estacion_id
    INTO v_bateria, v_estado, v_estacion_id
    FROM vehiculos
    WHERE id = NEW.vehiculo_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El vehículo no existe';
    END IF;
    IF v_bateria < 20 THEN
        RAISE EXCEPTION 'La batería del vehículo es inferior al 20%%';
    END IF;
    IF v_estado <> 'DISPONIBLE' OR v_estacion_id IS NULL THEN
        RAISE EXCEPTION 'El vehículo no está disponible en una estación';
    END IF;

    NEW.estacion_origen_id := v_estacion_id;
    UPDATE vehiculos SET estado = 'EN_USO', estacion_id = NULL WHERE id = NEW.vehiculo_id;
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
    v_operador_id BIGINT;
    v_motivo TEXT;
BEGIN
    IF NEW.estado IN ('EN_MANTENCION', 'DADO_DE_BAJA')
            AND OLD.estado IS DISTINCT FROM NEW.estado THEN
        v_operador_id := NULLIF(current_setting('app.operador', true), '')::bigint;
        v_motivo := NULLIF(trim(current_setting('app.motivo', true)), '');

        IF v_operador_id IS NULL THEN
            RAISE EXCEPTION 'Falta el operador responsable del cambio de estado';
        END IF;
        IF v_motivo IS NULL THEN
            RAISE EXCEPTION 'El motivo del cambio de estado es obligatorio';
        END IF;

        INSERT INTO auditoria_estado_vehiculo (
            vehiculo_id, estado_anterior, estado_nuevo, operador_id, motivo
        ) VALUES (
            NEW.id, OLD.estado, NEW.estado, v_operador_id, v_motivo
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditar_estado_vehiculo ON vehiculos;
CREATE TRIGGER trg_auditar_estado_vehiculo
    AFTER UPDATE OF estado ON vehiculos
    FOR EACH ROW
    EXECUTE FUNCTION fn_auditar_estado_vehiculo();

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
    v_precio_minuto NUMERIC(10, 2);
    v_costo_total NUMERIC(10, 2);
    v_saldo_actual NUMERIC(10, 2);
BEGIN
    SELECT j.usuario_id, j.vehiculo_id, j.iniciado_en, veh.tipo
    INTO v_usuario_id, v_vehiculo_id, v_iniciado_en, v_tipo_vehiculo
    FROM viajes j
    JOIN vehiculos veh ON j.vehiculo_id = veh.id
    WHERE j.id = p_viaje_id AND j.finalizado_en IS NULL
    FOR UPDATE OF j, veh;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El viaje no existe o ya fue finalizado';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM estaciones WHERE id = p_estacion_destino_id AND activa = TRUE
    ) THEN
        RAISE EXCEPTION 'La estación de destino no existe o está inactiva';
    END IF;

    v_minutos := GREATEST(CEIL(EXTRACT(EPOCH FROM (NOW() - v_iniciado_en)) / 60.0), 1);
    SELECT precio_por_minuto
    INTO v_precio_minuto
    FROM tarifas
    WHERE tipo_vehiculo = v_tipo_vehiculo
      AND activa = TRUE
      AND vigente_desde <= NOW()
      AND (vigente_hasta IS NULL OR vigente_hasta > NOW())
    ORDER BY vigente_desde DESC
    LIMIT 1;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe una tarifa vigente para el tipo %', v_tipo_vehiculo;
    END IF;

    v_costo_total := v_minutos * v_precio_minuto;
    SELECT saldo
    INTO v_saldo_actual
    FROM billeteras
    WHERE usuario_id = v_usuario_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El usuario no tiene una billetera asociada';
    END IF;

    UPDATE billeteras SET saldo = saldo - v_costo_total WHERE usuario_id = v_usuario_id;
    UPDATE viajes
    SET finalizado_en = NOW(),
        estacion_destino_id = p_estacion_destino_id,
        costo = v_costo_total
    WHERE id = p_viaje_id;
    UPDATE vehiculos
    SET estacion_id = p_estacion_destino_id, estado = 'DISPONIBLE'
    WHERE id = v_vehiculo_id;
END;
$$;

CREATE OR REPLACE PROCEDURE ejecutar_rebalanceo(
    p_solicitud_id UUID,
    p_estacion_origen_id BIGINT,
    p_estacion_destino_id BIGINT,
    p_tipo_vehiculo VARCHAR(50),
    p_cantidad INT,
    p_operador_id BIGINT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_disponibles INT;
    v_actualizados INT;
BEGIN
    IF p_solicitud_id IS NULL OR p_cantidad < 1 THEN
        RAISE EXCEPTION 'La solicitud y la cantidad deben ser válidas';
    END IF;
    IF p_estacion_origen_id = p_estacion_destino_id THEN
        RAISE EXCEPTION 'Las estaciones de origen y destino deben ser distintas';
    END IF;
    IF EXISTS (SELECT 1 FROM ordenes_traslado WHERE solicitud_id = p_solicitud_id) THEN
        RETURN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM estaciones WHERE id = p_estacion_origen_id AND activa) OR
       NOT EXISTS (SELECT 1 FROM estaciones WHERE id = p_estacion_destino_id AND activa) THEN
        RAISE EXCEPTION 'Las estaciones deben existir y estar activas';
    END IF;

    PERFORM id
    FROM estaciones
    WHERE id IN (p_estacion_origen_id, p_estacion_destino_id)
    ORDER BY id
    FOR UPDATE;

    WITH seleccion AS (
        SELECT id
        FROM vehiculos
        WHERE estacion_id = p_estacion_origen_id
          AND tipo = p_tipo_vehiculo
          AND estado = 'DISPONIBLE'
        ORDER BY id
        LIMIT p_cantidad
        FOR UPDATE SKIP LOCKED
    )
    UPDATE vehiculos
    SET estacion_id = p_estacion_destino_id
    WHERE id IN (SELECT id FROM seleccion);

    GET DIAGNOSTICS v_actualizados = ROW_COUNT;
    IF v_actualizados <> p_cantidad THEN
        RAISE EXCEPTION 'No hay suficientes vehículos disponibles en la estación de origen';
    END IF;

    INSERT INTO ordenes_traslado (
        solicitud_id, estacion_origen_id, estacion_destino_id,
        tipo_vehiculo, cantidad, operador_id
    ) VALUES (
        p_solicitud_id, p_estacion_origen_id, p_estacion_destino_id,
        p_tipo_vehiculo, p_cantidad, p_operador_id
    );
END;
$$;

REFRESH MATERIALIZED VIEW mv_demanda_estacion_horaria;
