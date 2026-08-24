package com.franquicias.infrastructure.config.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.franquicias.domain.model.RolUsuario;
import com.franquicias.domain.model.Usuario;
import com.franquicias.domain.port.UsuarioRepositoryPort;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class AdminUserSeederTest {

    @Mock
    private UsuarioRepositoryPort usuarioPort;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @Test
    void siembraElAdminSiNoExiste() {
        when(usuarioPort.findByUsername("admin")).thenReturn(Mono.empty());
        when(usuarioPort.save(any())).thenReturn(Mono.just(
            new Usuario(UUID.randomUUID(), "admin", "hash", "admin@test.com", RolUsuario.ADMIN, true, null)));

        AdminUserSeeder seeder = new AdminUserSeeder(usuarioPort, passwordEncoder, "admin", "pass123", "admin@test.com");
        seeder.run(null);

        verify(usuarioPort).save(any());
    }

    @Test
    void noSiembraElAdminSiYaExiste() {
        when(usuarioPort.findByUsername("admin")).thenReturn(Mono.just(
            new Usuario(UUID.randomUUID(), "admin", "hash", "admin@test.com", RolUsuario.ADMIN, true, null)));

        AdminUserSeeder seeder = new AdminUserSeeder(usuarioPort, passwordEncoder, "admin", "pass123", "admin@test.com");
        seeder.run(null);

        verify(usuarioPort, never()).save(any());
    }
}
