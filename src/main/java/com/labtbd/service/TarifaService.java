package com.labtbd.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.labtbd.dto.TarifaRequest;
import com.labtbd.entity.Tarifa;
import com.labtbd.repository.TarifaRepository;

@Service 
public class TarifaService {
    private final TarifaRepository repo;
    public TarifaService(TarifaRepository repo) {this.repo = repo;}

    public List<Tarifa> findAll() { return repo.findAll(); }

    public Tarifa findById(long id){
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "No se encuentra la Tarifa")
        );
    }
    
    public Tarifa create(TarifaRequest r){
        if (r.vigenteHasta() != null && !r.vigenteHasta().isAfter(r.vigenteDesde())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "vigenteHasta debe ser posterior a vigenteDesde");
        }
        return repo.create(new TarifaRepository.TarifaRequestData(
            r.tipoVehiculo(), r.precioPorMinuto(), r.vigenteDesde(), r.vigenteHasta()
        ));
    }

    public void deactivate(long id){
        if (repo.deactivate(id) == 0){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No se encuentra la Tarifa"
            );
        }
    }
}
