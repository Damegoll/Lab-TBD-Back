package com.labtbd.usuario;

/** Fila completa de la tabla usuarios. Nunca se serializa hacia el cliente (incluye el hash). */
public record Usuario(Long id, String username, String passwordHash, String rol) {
}
