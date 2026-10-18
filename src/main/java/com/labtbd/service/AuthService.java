package com.labtbd.service;

import com.labtbd.dto.AuthResponse;
import com.labtbd.dto.LoginRequest;
import com.labtbd.dto.RegisterRequest;
import com.labtbd.entity.Usuario;
import com.labtbd.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UsuarioService usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UsuarioService usuarios, PasswordEncoder encoder, JwtService jwt) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResponse login(LoginRequest req) {
        Usuario u = usuarios.buscarPorUsername(req.username())
                .filter(x -> encoder.matches(req.password(), x.passwordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
        return respuesta(u);
    }

    /** El registro público siempre crea usuarios con rol USER. Un ADMIN solo se crea por configuración. */
    public AuthResponse register(RegisterRequest req) {
        return respuesta(usuarios.registrar(req.username(), req.password(), "USER"));
    }

    private AuthResponse respuesta(Usuario u) {
        return new AuthResponse(jwt.generar(u.username(), u.rol()), u.username(), u.rol());
    }
}
