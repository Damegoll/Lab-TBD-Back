package com.labtbd.repository;

import com.labtbd.entity.AuditoriaEstadoVehiculo;
import com.labtbd.entity.EstadoVehiculo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class AuditoriaRepository {
    private final JdbcTemplate jdbc;

    private final RowMapper<AuditoriaEstadoVehiculo> mapper = (rs, n) -> {
        String anterior = rs.getString("estado_anterior");
        return new AuditoriaEstadoVehiculo(
                rs.getLong("id"),
                rs.getLong("vehiculo_id"),
                anterior == null ? null : EstadoVehiculo.valueOf(anterior),
                EstadoVehiculo.valueOf(rs.getString("estado_nuevo")),
                rs.getString("operador"),
                rs.getObject("fecha", java.time.OffsetDateTime.class),
                rs.getString("motivo"));
    };

    public AuditoriaRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<AuditoriaEstadoVehiculo> findByVehiculoId(long vehiculoId) {
        return jdbc.query("""
                SELECT a.id, a.vehiculo_id, a.estado_anterior, a.estado_nuevo,
                       COALESCE(u.username, 'SISTEMA') AS operador, a.fecha, a.motivo
                FROM auditoria_estado_vehiculo a
                LEFT JOIN usuarios u ON u.id = a.operador_id
                WHERE a.vehiculo_id = ?
                ORDER BY a.fecha DESC
                """, mapper, vehiculoId);
    }
}