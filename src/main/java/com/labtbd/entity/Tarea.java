package com.labtbd.entity;

import java.time.Instant;

/** Fila de la tabla tareas. Sin ORM: es un objeto plano que el repositorio llena a mano. */
public record Tarea(Long id, String titulo, String descripcion, boolean completada, Instant creadaEn) {
}
