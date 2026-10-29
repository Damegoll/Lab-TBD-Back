package com.labtbd.controller;

import com.labtbd.dto.EstacionDisponibilidadResponse;
import com.labtbd.service.VehiculoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/estaciones")
public class EstacionController {

    private final VehiculoService vehiculoService;

    public EstacionController(VehiculoService vehiculoService) {
        this.vehiculoService = vehiculoService;
    }

    @GetMapping("/disponibles")
    @PreAuthorize("hasAnyRole('USUARIO', 'OPERADOR', 'ADMIN')")
    public List<EstacionDisponibilidadResponse> listarDisponibles() {
        return vehiculoService.obtenerEstacionesDisponibles();
    }
}