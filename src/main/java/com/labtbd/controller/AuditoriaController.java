package com.labtbd.controller;

import com.labtbd.dto.AuditoriaResponse;
import com.labtbd.service.AuditoriaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vehiculos/{id}/auditoria")
public class AuditoriaController {
    private final AuditoriaService service;
    public AuditoriaController(AuditoriaService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERADOR','ADMIN')")
    public List<AuditoriaResponse> findByVehiculo(@PathVariable long id) {
        return service.findByVehiculoId(id).stream().map(AuditoriaResponse::desde).toList();
    }
}