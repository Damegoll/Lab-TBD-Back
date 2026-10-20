package com.labtbd.service;

import com.labtbd.dto.UsuarioCreateRequest;
import com.labtbd.dto.UsuarioUpdateRequest;
import com.labtbd.entity.Usuario;
import com.labtbd.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository repository,
            PasswordEncoder passwordEncoder
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
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
}