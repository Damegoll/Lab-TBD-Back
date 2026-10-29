package com.labtbd.service;

import com.labtbd.dto.EstacionDisponibilidadResponse;
import com.labtbd.dto.VehiculoCreateRequest;
import com.labtbd.dto.VehiculoUpdateRequest;
import com.labtbd.dto.CambiarEstadoRequest;
import com.labtbd.entity.Vehiculo;
import com.labtbd.entity.EstadoVehiculo;
import com.labtbd.repository.VehiculoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class VehiculoService {
    private final VehiculoRepository repo;
    private final JdbcTemplate jdbcTemplate;

    public VehiculoService(VehiculoRepository repo, JdbcTemplate jdbcTemplate) { this.repo = repo;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Vehiculo> findAll() { return repo.findAll(); }

    public Vehiculo findById(long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Vehículo no encontrado"));
    }

    public Vehiculo create(VehiculoCreateRequest r) {
        if (repo.existsByCode(r.codigo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El código del vehículo ya existe");
        }
        return repo.create(new VehiculoRepository.VehiculoCreateRequestData(
                r.codigo(), r.tipo(), r.bateria(), r.estacionId()));
    }

    public void update(long id, VehiculoUpdateRequest r) {
        if (repo.update(id, r.codigo(), r.bateria()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Vehículo no encontrado o dado de baja");
        }
    }

    @Transactional
    public void changeState(long id, CambiarEstadoRequest r, String operador) {
        if (r.estado() != EstadoVehiculo.EN_MANTENCION
                && r.estado() != EstadoVehiculo.DADO_DE_BAJA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Este endpoint solo permite enviar a mantención o dar de baja");
        }
        if (repo.changeState(id, r.estado(), operador, r.motivo()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Vehículo no encontrado");
        }
    }

    @Scheduled(cron = "0 0 * * * *") // Se ejecuta al inicio de cada hora
    public void refrescarVistaDemanda() {
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_demanda_estacion_horaria");
    }

    public List<EstacionDisponibilidadResponse> obtenerEstacionesDisponibles() {
        String sql = """
        SELECT 
            e.id AS estacion_id,
            e.nombre AS estacion_nombre,
            v.tipo AS tipo_vehiculo,
            COUNT(v.id) AS cantidad,
            ROUND(AVG(v.bateria), 2) AS bateria_promedio
        FROM estaciones e
        JOIN vehiculos v ON e.id = v.estacion_id
        WHERE v.estado = 'DISPONIBLE' AND e.activa = true
        GROUP BY e.id, e.nombre, v.tipo
        ORDER BY e.nombre, v.tipo
    """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new EstacionDisponibilidadResponse(
                rs.getLong("estacion_id"),
                rs.getString("estacion_nombre"),
                rs.getString("tipo_vehiculo"),
                rs.getLong("cantidad"),
                rs.getDouble("bateria_promedio")
        ));
    }
}