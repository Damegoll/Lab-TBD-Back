package com.labtbd.security;

import com.labtbd.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Crea el usuario ADMIN inicial al arrancar si todavía no existe. */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;

    public AdminInitializer(UsuarioRepository usuarios,
                            PasswordEncoder encoder,
                            @Value("${app.admin.username}") String username,
                            @Value("${app.admin.password}") String password) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!usuarios.existsByUsername(username)) {
            usuarios.crear(username, encoder.encode(password), "ADMIN");
            log.info("Usuario ADMIN '{}' creado", username);
        }
    }
}
