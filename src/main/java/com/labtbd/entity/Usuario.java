package com.labtbd.entity;

/** Fila de la tabla usuarios. Incluye el hash, por eso nunca se devuelve tal cual al cliente (ver UsuarioResponse). */
public record Usuario(Long id, String username, String passwordHash, String rol) {
}
