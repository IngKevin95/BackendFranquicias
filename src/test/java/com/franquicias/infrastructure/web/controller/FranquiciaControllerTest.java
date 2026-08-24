package com.franquicias.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.franquicias.AbstractIntegrationTest;
import com.franquicias.infrastructure.web.dto.FranquiciaResponse;
import com.franquicias.infrastructure.web.dto.NombreRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.franquicias.infrastructure.config.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FranquiciaControllerTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        String token = jwtUtil.generateToken("admin");
        webTestClient = webTestClient.mutate().defaultHeader("Authorization", "Bearer " + token).build();
    }

    @Test
    void creaUnaFranquiciaYLaRenombra() {
        FranquiciaResponse creada = webTestClient.post().uri("/api/v1/franquicias")
            .bodyValue(new NombreRequest("Frutería Don Pepe"))
            .exchange()
            .expectStatus().isCreated()
            .expectBody(FranquiciaResponse.class)
            .returnResult().getResponseBody();

        assertThat(creada.id()).isNotNull();
        assertThat(creada.nombre()).isEqualTo("Frutería Don Pepe");

        webTestClient.patch().uri("/api/v1/franquicias/" + creada.id())
            .bodyValue(new NombreRequest("Frutería Don Pepe Renovada"))
            .exchange()
            .expectStatus().isOk()
            .expectBody(FranquiciaResponse.class)
            .value(f -> assertThat(f.nombre()).isEqualTo("Frutería Don Pepe Renovada"));
    }

    @Test
    void retorna404AlRenombrarFranquiciaInexistente() {
        webTestClient.patch().uri("/api/v1/franquicias/" + UUID.randomUUID())
            .bodyValue(new NombreRequest("No importa"))
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void retorna400AlCrearFranquiciaConNombreVacio() {
        webTestClient.post().uri("/api/v1/franquicias")
            .bodyValue(new NombreRequest(""))
            .exchange()
            .expectStatus().isBadRequest();
    }
}
