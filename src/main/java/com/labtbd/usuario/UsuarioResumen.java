package com.labtbd.usuario;

/** Vista pública de un usuario (sin hash de contraseña). */
public record UsuarioResumen(Long id, String username, String rol) {
}
