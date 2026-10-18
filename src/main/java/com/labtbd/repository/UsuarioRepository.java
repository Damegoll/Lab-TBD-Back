package com.labtbd.repository;

import com.labtbd.entity.Usuario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UsuarioRepository {

    private static final String COLUMNAS = "id, username, password_hash, rol";

    private static final RowMapper<Usuario> MAPPER = (rs, i) -> new Usuario(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("rol"));

    private final JdbcTemplate jdbc;

    public UsuarioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Usuario> listar() {
        return jdbc.query("SELECT " + COLUMNAS + " FROM usuarios ORDER BY id", MAPPER);
    }

    public Optional<Usuario> buscarPorUsername(String username) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM usuarios WHERE username = ?", MAPPER, username)
                .stream().findFirst();
    }

    public boolean existePorUsername(String username) {
        Boolean existe = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM usuarios WHERE username = ?)", Boolean.class, username);
        return Boolean.TRUE.equals(existe);
    }

    public Usuario crear(String username, String passwordHash, String rol) {
        return jdbc.queryForObject(
                "INSERT INTO usuarios (username, password_hash, rol) VALUES (?, ?, ?) RETURNING " + COLUMNAS,
                MAPPER, username, passwordHash, rol);
    }
}
