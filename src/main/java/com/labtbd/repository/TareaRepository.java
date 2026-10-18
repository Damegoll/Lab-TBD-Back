package com.labtbd.repository;

import com.labtbd.entity.Tarea;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Acceso a datos con SQL explícito sobre JdbcTemplate (sin ORM). */
@Repository
public class TareaRepository {

    private static final String COLUMNAS = "id, titulo, descripcion, completada, creada_en";

    private static final RowMapper<Tarea> MAPPER = (rs, i) -> new Tarea(
            rs.getLong("id"),
            rs.getString("titulo"),
            rs.getString("descripcion"),
            rs.getBoolean("completada"),
            rs.getTimestamp("creada_en").toInstant());

    private final JdbcTemplate jdbc;

    public TareaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Tarea> listar() {
        return jdbc.query("SELECT " + COLUMNAS + " FROM tareas ORDER BY id", MAPPER);
    }

    public Optional<Tarea> buscar(long id) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM tareas WHERE id = ?", MAPPER, id)
                .stream().findFirst();
    }

    public Tarea crear(String titulo, String descripcion, boolean completada) {
        return jdbc.queryForObject(
                "INSERT INTO tareas (titulo, descripcion, completada) VALUES (?, ?, ?) RETURNING " + COLUMNAS,
                MAPPER, titulo, descripcion, completada);
    }

    public Optional<Tarea> actualizar(long id, String titulo, String descripcion, boolean completada) {
        return jdbc.query(
                "UPDATE tareas SET titulo = ?, descripcion = ?, completada = ? WHERE id = ? RETURNING " + COLUMNAS,
                MAPPER, titulo, descripcion, completada, id)
                .stream().findFirst();
    }

    public boolean eliminar(long id) {
        return jdbc.update("DELETE FROM tareas WHERE id = ?", id) > 0;
    }
}
