package com.franquicias.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.franquicias.AbstractIntegrationTest;
import com.franquicias.application.service.FranquiciaService;
import com.franquicias.application.service.SucursalService;
import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.model.Sucursal;
import com.franquicias.infrastructure.web.dto.CrearProductoRequest;
import com.franquicias.infrastructure.web.dto.ModificarStockRequest;
import com.franquicias.infrastructure.web.dto.NombreRequest;
import com.franquicias.infrastructure.web.dto.ProductoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductoControllerTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private FranquiciaService franquiciaService;
    @Autowired
    private SucursalService sucursalService;

    private Franquicia franquicia;
    private Sucursal sucursal;

    @BeforeEach
    void crearFixtures() {
        franquicia = franquiciaService.crear("Frutería Don Pepe").block();
        sucursal = sucursalService.agregar(franquicia.id(), "Sede Norte").block();
    }

    private String basePath() {
        return "/api/v1/franquicias/" + franquicia.id() + "/sucursales/" + sucursal.id() + "/productos";
    }

    @Test
    void agregaModificaYEliminaUnProducto() {
        ProductoResponse creado = webTestClient.post().uri(basePath())
            .bodyValue(new CrearProductoRequest("Manzana", 10))
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ProductoResponse.class)
            .returnResult().getResponseBody();

        assertThat(creado.stock()).isEqualTo(10);

        webTestClient.patch().uri(basePath() + "/" + creado.id() + "/stock")
            .bodyValue(new ModificarStockRequest(50))
            .exchange()
            .expectStatus().isOk()
            .expectBody(ProductoResponse.class)
            .value(p -> assertThat(p.stock()).isEqualTo(50));

        webTestClient.patch().uri(basePath() + "/" + creado.id())
            .bodyValue(new NombreRequest("Manzana Roja"))
            .exchange()
            .expectStatus().isOk()
            .expectBody(ProductoResponse.class)
            .value(p -> assertThat(p.nombre()).isEqualTo("Manzana Roja"));

        webTestClient.delete().uri(basePath() + "/" + creado.id())
            .exchange()
            .expectStatus().isNoContent();
    }

    @Test
    void retorna400AlModificarStockANegativo() {
        ProductoResponse creado = webTestClient.post().uri(basePath())
            .bodyValue(new CrearProductoRequest("Manzana", 10))
            .exchange()
            .expectBody(ProductoResponse.class)
            .returnResult().getResponseBody();

        webTestClient.patch().uri(basePath() + "/" + creado.id() + "/stock")
            .bodyValue(new ModificarStockRequest(-5))
            .exchange()
            .expectStatus().isBadRequest();
    }
}
