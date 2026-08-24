package com.franquicias.infrastructure.web.controller;

import com.franquicias.AbstractIntegrationTest;
import com.franquicias.infrastructure.config.security.JwtUtil;
import com.franquicias.infrastructure.web.dto.auth.RegisterRequest;
import com.franquicias.domain.model.RolUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UsuarioControllerTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtUtil jwtUtil;

    private String adminToken;
    private String writeToken;
    private String readToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtUtil.generateToken("admin", "ADMIN");
        writeToken = jwtUtil.generateToken("writer", "WRITE");
        readToken  = jwtUtil.generateToken("reader", "READ");
    }

    @Test
    void crearUsuario_conAdmin_returns201() {
        webTestClient.post().uri("/api/v1/usuarios")
            .header("Authorization", "Bearer " + adminToken)
            .bodyValue(new RegisterRequest("newuser", "pass123", "new@test.com", RolUsuario.READ))
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.username").isEqualTo("newuser")
            .jsonPath("$.role").isEqualTo("READ");
    }

    @Test
    void crearUsuario_conWrite_returns403() {
        webTestClient.post().uri("/api/v1/usuarios")
            .header("Authorization", "Bearer " + writeToken)
            .bodyValue(new RegisterRequest("otro", "pass123", "otro@test.com", RolUsuario.READ))
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    void crearUsuario_conRead_returns403() {
        webTestClient.post().uri("/api/v1/usuarios")
            .header("Authorization", "Bearer " + readToken)
            .bodyValue(new RegisterRequest("otro2", "pass123", "otro2@test.com", RolUsuario.READ))
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    void listarUsuarios_conAdmin_returns200() {
        webTestClient.post().uri("/api/v1/usuarios")
            .header("Authorization", "Bearer " + adminToken)
            .bodyValue(new RegisterRequest("listado1", "pass123", "listado1@test.com", RolUsuario.READ))
            .exchange()
            .expectStatus().isCreated();

        webTestClient.get().uri("/api/v1/usuarios")
            .header("Authorization", "Bearer " + adminToken)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(com.franquicias.infrastructure.web.dto.UsuarioResponse.class)
            .value(list -> org.assertj.core.api.Assertions.assertThat(list)
                .extracting(com.franquicias.infrastructure.web.dto.UsuarioResponse::username)
                .contains("listado1"));
    }

    @Test
    void listarUsuarios_conWrite_returns403() {
        webTestClient.get().uri("/api/v1/usuarios")
            .header("Authorization", "Bearer " + writeToken)
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    void listarUsuarios_conRead_returns403() {
        webTestClient.get().uri("/api/v1/usuarios")
            .header("Authorization", "Bearer " + readToken)
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    void listarUsuarios_sinToken_returns401() {
        webTestClient.get().uri("/api/v1/usuarios")
            .exchange()
            .expectStatus().isUnauthorized();
    }
}
