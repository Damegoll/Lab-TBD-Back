package com.labtbd.service;

import com.labtbd.dto.ResumenMensualResponse;
import com.labtbd.dto.UsuarioCreateRequest;
import com.labtbd.dto.UsuarioUpdateRequest;
import com.labtbd.entity.Usuario;
import com.labtbd.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public UsuarioService(
            UsuarioRepository repository,
            PasswordEncoder passwordEncoder, JdbcTemplate jdbcTemplate
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Usuario> findAll() {
        return repository.findAll();
    }

    public Usuario findById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    public Usuario findByUsername(String username) {
        return repository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    public Usuario create(UsuarioCreateRequest request) {
        if (repository.existsByUsername(request.username())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El username ya está en uso");
        }

        return repository.create(
                request.username(),
                request.nombre(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.rol()
        );
    }

    public void update(long id, UsuarioUpdateRequest request) {
        int updated = repository.update(
                id,
                request.nombre(),
                request.email(),
                request.rol()
        );

        if (updated == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Usuario no encontrado o inactivo");
        }
    }

    public void deactivate(long id) {
        int updated = repository.deactivate(id);

        if (updated == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Usuario no encontrado o ya inactivo");
        }
    }

    public List<ResumenMensualResponse> obtenerResumenMensual(Long usuarioId) {
        String sql = """
        SELECT 
            EXTRACT(YEAR FROM iniciado_en) AS anio,
            EXTRACT(MONTH FROM iniciado_en) AS mes,
            COUNT(id) AS cantidad_viajes,
            ROUND(SUM(EXTRACT(EPOCH FROM (finalizado_en - iniciado_en)) / 60)::numeric, 2) AS minutos_totales,
            SUM(costo) AS gasto_total
        FROM viajes
        WHERE usuario_id = ?
        GROUP BY EXTRACT(YEAR FROM iniciado_en), EXTRACT(MONTH FROM iniciado_en)
        ORDER BY anio DESC, mes DESC
    """;

        return jdbcTemplate.query(sql, new DataClassRowMapper<>(ResumenMensualResponse.class), usuarioId);
    }
}