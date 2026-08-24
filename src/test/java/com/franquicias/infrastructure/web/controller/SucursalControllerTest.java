package com.franquicias.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.franquicias.AbstractIntegrationTest;
import com.franquicias.application.service.FranquiciaService;
import com.franquicias.domain.model.Franquicia;
import com.franquicias.infrastructure.web.dto.NombreRequest;
import com.franquicias.infrastructure.web.dto.SucursalResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.franquicias.infrastructure.config.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SucursalControllerTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private FranquiciaService franquiciaService;
    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        String token = jwtUtil.generateToken("admin", "ADMIN");
        webTestClient = webTestClient.mutate().defaultHeader("Authorization", "Bearer " + token).build();
    }

    @Test
    void agregaUnaSucursalYLaRenombra() {
        Franquicia franquicia = franquiciaService.crear("Frutería Don Pepe").block();

        SucursalResponse creada = webTestClient.post()
            .uri("/api/v1/franquicias/" + franquicia.id() + "/sucursales")
            .bodyValue(new NombreRequest("Sede Norte"))
            .exchange()
            .expectStatus().isCreated()
            .expectBody(SucursalResponse.class)
            .returnResult().getResponseBody();

        assertThat(creada.franquiciaId()).isEqualTo(franquicia.id());

        webTestClient.patch()
            .uri("/api/v1/franquicias/" + franquicia.id() + "/sucursales/" + creada.id())
            .bodyValue(new NombreRequest("Sede Norte Renovada"))
            .exchange()
            .expectStatus().isOk()
            .expectBody(SucursalResponse.class)
            .value(s -> assertThat(s.nombre()).isEqualTo("Sede Norte Renovada"));
    }

    @Test
    void retorna404AlAgregarSucursalAFranquiciaInexistente() {
        webTestClient.post()
            .uri("/api/v1/franquicias/" + java.util.UUID.randomUUID() + "/sucursales")
            .bodyValue(new NombreRequest("Sede Norte"))
            .exchange()
            .expectStatus().isNotFound();
    }
}
