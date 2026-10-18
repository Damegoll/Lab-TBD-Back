package com.labtbd.security;

import com.labtbd.usuario.Usuario;
import com.labtbd.usuario.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record AuthResponse(String token, String username, String rol) {
    }

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        Usuario u = usuarios.findByUsername(req.username())
                .filter(x -> encoder.matches(req.password(), x.passwordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
        return respuesta(u);
    }

    /** El registro público siempre crea usuarios con rol USER. Un ADMIN solo se crea por configuración. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        try {
            if (usuarios.existsByUsername(req.username())) {
                throw yaExiste();
            }
            return respuesta(usuarios.crear(req.username(), encoder.encode(req.password()), "USER"));
        } catch (DuplicateKeyException e) {
            throw yaExiste();
        }
    }

    private AuthResponse respuesta(Usuario u) {
        return new AuthResponse(jwt.generar(u.username(), u.rol()), u.username(), u.rol());
    }

    private static ResponseStatusException yaExiste() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de usuario ya existe");
    }
}
