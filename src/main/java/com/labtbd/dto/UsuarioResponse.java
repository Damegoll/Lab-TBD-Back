package com.labtbd.dto;

import com.labtbd.entity.Usuario;

/** Vista pública de un usuario (sin hash de contraseña). */
public record UsuarioResponse(Long id, String username, String rol) {

    public static UsuarioResponse desde(Usuario u) {
        return new UsuarioResponse(u.id(), u.username(), u.rol());
    }
}
