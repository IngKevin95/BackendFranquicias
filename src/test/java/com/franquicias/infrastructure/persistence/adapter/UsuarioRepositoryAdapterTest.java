package com.franquicias.infrastructure.persistence.adapter;

import com.franquicias.AbstractIntegrationTest;
import com.franquicias.domain.model.RolUsuario;
import com.franquicias.domain.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

@SpringBootTest
class UsuarioRepositoryAdapterTest extends AbstractIntegrationTest {

    @Autowired
    private UsuarioRepositoryAdapter adapter;

    @Test
    void findByUsername_adminSeedExists() {
        StepVerifier.create(adapter.findByUsername("admin"))
            .assertNext(u -> {
                assert u.username().equals("admin");
                assert u.role() == RolUsuario.ADMIN;
                assert u.activo();
            })
            .verifyComplete();
    }

    @Test
    void save_and_findByUsername() {
        Usuario nuevo = new Usuario(null, "testuser", "$2a$10$hash", "test@test.com", RolUsuario.READ, true, null);
        StepVerifier.create(adapter.save(nuevo).flatMap(saved -> adapter.findByUsername("testuser")))
            .assertNext(u -> {
                assert u.username().equals("testuser");
                assert u.role() == RolUsuario.READ;
            })
            .verifyComplete();
    }
}
