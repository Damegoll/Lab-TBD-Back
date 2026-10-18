package com.labtbd.tarea;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * CRUD de ejemplo. Permisos (ver SecurityConfig):
 * GET/POST/PUT -> USER o ADMIN; DELETE -> solo ADMIN.
 */
@RestController
@RequestMapping("/api/tareas")
public class TareaController {

    private final TareaRepository repository;

    public TareaController(TareaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Tarea> listar() {
        return repository.listar();
    }

    @GetMapping("/{id}")
    public Tarea obtener(@PathVariable long id) {
        return repository.buscar(id).orElseThrow(TareaController::noEncontrada);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Tarea crear(@Valid @RequestBody TareaRequest req) {
        return repository.crear(req);
    }

    @PutMapping("/{id}")
    public Tarea actualizar(@PathVariable long id, @Valid @RequestBody TareaRequest req) {
        return repository.actualizar(id, req).orElseThrow(TareaController::noEncontrada);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable long id) {
        if (!repository.eliminar(id)) {
            throw noEncontrada();
        }
    }

    private static ResponseStatusException noEncontrada() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada");
    }
}
