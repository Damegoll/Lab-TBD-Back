package com.labtbd.repository;

import com.labtbd.entity.OrdenTraslado;
import com.labtbd.entity.TipoVehiculo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OrdenTrasladoRepository {
    private final JdbcTemplate jdbc;

    private final RowMapper<OrdenTraslado> mapper = (rs, n) -> new OrdenTraslado(
            rs.getLong("id"),
            rs.getObject("solicitud_id", UUID.class),
            rs.getLong("estacion_origen_id"),
            rs.getLong("estacion_destino_id"),
            TipoVehiculo.valueOf(rs.getString("tipo_vehiculo")),
            rs.getInt("cantidad"),
            rs.getLong("operador_id"),
            rs.getObject("creada_en", java.time.OffsetDateTime.class));

    private static final String COLUMNS = """
            id, solicitud_id, estacion_origen_id, estacion_destino_id,
            tipo_vehiculo, cantidad, operador_id, creada_en
            """;

    public OrdenTrasladoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<OrdenTraslado> findAll() {
        return jdbc.query("SELECT " + COLUMNS + " FROM ordenes_traslado ORDER BY id DESC", mapper);
    }

    public Optional<OrdenTraslado> findById(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM ordenes_traslado WHERE id = ?",
                mapper, id).stream().findFirst();
    }

    public Optional<OrdenTraslado> findBySolicitudId(UUID solicitudId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM ordenes_traslado WHERE solicitud_id = ?",
                mapper, solicitudId).stream().findFirst();
    }

    public void ejecutarRebalanceo(UUID solicitudId, long origen, long destino,
                                   String tipo, int cantidad, long operadorId) {
        jdbc.update("CALL ejecutar_rebalanceo(?, ?, ?, ?, ?, ?)",
                solicitudId, origen, destino, tipo, cantidad, operadorId);
    }

    public Optional<Long> findOperadorId(String username) {
        return jdbc.queryForList("SELECT id FROM usuarios WHERE username = ?",
                Long.class, username).stream().findFirst();
    }
}