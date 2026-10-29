package com.labtbd.service;

import com.labtbd.entity.AuditoriaEstadoVehiculo;
import com.labtbd.repository.AuditoriaRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AuditoriaService {
    private final AuditoriaRepository repo;
    private final VehiculoService vehiculos;

    public AuditoriaService(AuditoriaRepository repo, VehiculoService vehiculos) {
        this.repo = repo;
        this.vehiculos = vehiculos;
    }

    public List<AuditoriaEstadoVehiculo> findByVehiculoId(long vehiculoId) {
        vehiculos.findById(vehiculoId);
        return repo.findByVehiculoId(vehiculoId);
    }
}