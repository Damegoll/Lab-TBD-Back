package com.labtbd.config;

import com.labtbd.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Crea el usuario ADMIN inicial al arrancar si todavía no existe. */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UsuarioService usuarios;
    private final String username;
    private final String password;

    public AdminInitializer(UsuarioService usuarios,
                            @Value("${app.admin.username}") String username,
                            @Value("${app.admin.password}") String password) {
        this.usuarios = usuarios;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!usuarios.existe(username)) {
            usuarios.registrar(username, password, "ADMIN");
            log.info("Usuario ADMIN '{}' creado", username);
        }
    }
}
