package com.franquicias.infrastructure.config.security;

import com.franquicias.domain.model.RolUsuario;
import com.franquicias.domain.model.Usuario;
import com.franquicias.domain.port.UsuarioRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AdminUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);

    private final UsuarioRepositoryPort usuarioPort;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private final String adminEmail;

    public AdminUserSeeder(UsuarioRepositoryPort usuarioPort, BCryptPasswordEncoder passwordEncoder,
                            @Value("${app.admin.username}") String adminUsername,
                            @Value("${app.admin.password}") String adminPassword,
                            @Value("${app.admin.email}") String adminEmail) {
        this.usuarioPort = usuarioPort;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.adminEmail = adminEmail;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Bloqueante deliberadamente: corre una sola vez durante el arranque,
        // antes de que la app empiece a aceptar requests. Los tests dependen
        // de que el admin ya exista apenas termina el context refresh.
        usuarioPort.findByUsername(adminUsername)
            .switchIfEmpty(Mono.defer(() -> {
                Usuario admin = new Usuario(null, adminUsername, passwordEncoder.encode(adminPassword),
                    adminEmail, RolUsuario.ADMIN, true, null);
                log.info("Sembrando usuario admin inicial '{}'", adminUsername);
                return usuarioPort.save(admin);
            }))
            .block();
    }
}
