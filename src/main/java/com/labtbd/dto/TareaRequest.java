package com.labtbd.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TareaRequest(
        @NotBlank @Size(max = 150) String titulo,
        @Size(max = 1000) String descripcion,
        Boolean completada) {

    public boolean estaCompletada() {
        return Boolean.TRUE.equals(completada);
    }
}
