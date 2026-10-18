package com.labtbd.service;

import com.labtbd.dto.TareaRequest;
import com.labtbd.dto.TareaResponse;
import com.labtbd.entity.Tarea;
import com.labtbd.repository.TareaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TareaService {

    private final TareaRepository repository;

    public TareaService(TareaRepository repository) {
        this.repository = repository;
    }

    public List<TareaResponse> listar() {
        return repository.listar().stream().map(TareaResponse::desde).toList();
    }

    public TareaResponse obtener(long id) {
        return TareaResponse.desde(buscar(id));
    }

    public TareaResponse crear(TareaRequest req) {
        Tarea tarea = repository.crear(req.titulo(), req.descripcion(), req.estaCompletada());
        return TareaResponse.desde(tarea);
    }

    public TareaResponse actualizar(long id, TareaRequest req) {
        Tarea tarea = repository.actualizar(id, req.titulo(), req.descripcion(), req.estaCompletada())
                .orElseThrow(TareaService::noEncontrada);
        return TareaResponse.desde(tarea);
    }

    public void eliminar(long id) {
        if (!repository.eliminar(id)) {
            throw noEncontrada();
        }
    }

    private Tarea buscar(long id) {
        return repository.buscar(id).orElseThrow(TareaService::noEncontrada);
    }

    private static ResponseStatusException noEncontrada() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada");
    }
}
