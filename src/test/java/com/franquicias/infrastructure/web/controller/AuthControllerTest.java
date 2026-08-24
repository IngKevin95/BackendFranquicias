package com.franquicias.infrastructure.web.controller;

import com.franquicias.AbstractIntegrationTest;
import com.franquicias.infrastructure.web.dto.auth.AuthRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void login_adminCredentials_returnsToken() {
        webTestClient.post().uri("/api/v1/auth/login")
            .bodyValue(new AuthRequest("admin", "admin123"))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.token").isNotEmpty();
    }

    @Test
    void login_wrongPassword_returns401() {
        webTestClient.post().uri("/api/v1/auth/login")
            .bodyValue(new AuthRequest("admin", "wrong"))
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void login_unknownUser_returns401() {
        webTestClient.post().uri("/api/v1/auth/login")
            .bodyValue(new AuthRequest("noexiste", "pass"))
            .exchange()
            .expectStatus().isUnauthorized();
    }
}
