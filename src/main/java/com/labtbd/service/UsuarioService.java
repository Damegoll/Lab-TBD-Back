package com.labtbd.service;

import com.labtbd.dto.UsuarioResponse;
import com.labtbd.entity.Usuario;
import com.labtbd.repository.UsuarioRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    public List<UsuarioResponse> listar() {
        return repository.listar().stream().map(UsuarioResponse::desde).toList();
    }

    public Optional<Usuario> buscarPorUsername(String username) {
        return repository.buscarPorUsername(username);
    }

    public boolean existe(String username) {
        return repository.existePorUsername(username);
    }

    /** Hashea la contraseña con BCrypt y guarda el usuario. Responde 409 si el nombre ya existe. */
    public Usuario registrar(String username, String passwordPlano, String rol) {
        if (repository.existePorUsername(username)) {
            throw yaExiste();
        }
        try {
            return repository.crear(username, encoder.encode(passwordPlano), rol);
        } catch (DuplicateKeyException e) {
            throw yaExiste(); // carrera entre dos registros simultáneos
        }
    }

    private static ResponseStatusException yaExiste() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de usuario ya existe");
    }
}
