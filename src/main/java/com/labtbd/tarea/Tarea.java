package com.labtbd.tarea;

import java.time.Instant;

public record Tarea(Long id, String titulo, String descripcion, boolean completada, Instant creadaEn) {
}
