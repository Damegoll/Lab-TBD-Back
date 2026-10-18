package com.labtbd.dto;

import com.labtbd.entity.Tarea;

import java.time.Instant;

public record TareaResponse(Long id, String titulo, String descripcion, boolean completada, Instant creadaEn) {

    public static TareaResponse desde(Tarea t) {
        return new TareaResponse(t.id(), t.titulo(), t.descripcion(), t.completada(), t.creadaEn());
    }
}
