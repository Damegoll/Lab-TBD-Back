package com.labtbd.controller;

import com.labtbd.dto.TareaRequest;
import com.labtbd.dto.TareaResponse;
import com.labtbd.service.TareaService;
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

import java.util.List;

/**
 * CRUD de ejemplo. Permisos (ver SecurityConfig):
 * GET/POST/PUT -> USER o ADMIN; DELETE -> solo ADMIN.
 */
@RestController
@RequestMapping("/api/tareas")
public class TareaController {

    private final TareaService service;

    public TareaController(TareaService service) {
        this.service = service;
    }

    @GetMapping
    public List<TareaResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public TareaResponse obtener(@PathVariable long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TareaResponse crear(@Valid @RequestBody TareaRequest req) {
        return service.crear(req);
    }

    @PutMapping("/{id}")
    public TareaResponse actualizar(@PathVariable long id, @Valid @RequestBody TareaRequest req) {
        return service.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable long id) {
        service.eliminar(id);
    }
}
