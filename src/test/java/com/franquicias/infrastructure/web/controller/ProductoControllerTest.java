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
            .bodyValue(new CrearProductoRequest("Manzana"))
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ProductoResponse.class)
            .returnResult().getResponseBody();

        assertThat(creado.stock()).isEqualTo(0);

        webTestClient.patch().uri(basePath() + "/" + creado.id() + "/stock")
            .bodyValue(new ModificarStockRequest("ENTRADA", 50))
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
    void retorna400AlModificarStockACeroONegativo() {
        ProductoResponse creado = webTestClient.post().uri(basePath())
            .bodyValue(new CrearProductoRequest("Manzana"))
            .exchange()
            .expectBody(ProductoResponse.class)
            .returnResult().getResponseBody();

        webTestClient.patch().uri(basePath() + "/" + creado.id() + "/stock")
            .bodyValue(new ModificarStockRequest("ENTRADA", -5))
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void retorna400AlIntentarSacarMasStockDelExistente() {
        ProductoResponse creado = webTestClient.post().uri(basePath())
            .bodyValue(new CrearProductoRequest("Pera"))
            .exchange()
            .expectBody(ProductoResponse.class)
            .returnResult().getResponseBody();

        // Agregamos stock para poder restar luego
        webTestClient.patch().uri(basePath() + "/" + creado.id() + "/stock")
            .bodyValue(new ModificarStockRequest("ENTRADA", 10))
            .exchange()
            .expectStatus().isOk();

        webTestClient.patch().uri(basePath() + "/" + creado.id() + "/stock")
            .bodyValue(new ModificarStockRequest("SALIDA", 15))
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void retornaElProductoConMasStockPorCadaSucursal() {
        Sucursal sedeSur = sucursalService.agregar(franquicia.id(), "Sede Sur").block();

        // Manzana en Sede Norte (stock 10)
        ProductoResponse p1 = webTestClient.post().uri(basePath())
            .bodyValue(new CrearProductoRequest("Manzana"))
            .exchange().expectBody(ProductoResponse.class).returnResult().getResponseBody();
        webTestClient.patch().uri(basePath() + "/" + p1.id() + "/stock")
            .bodyValue(new ModificarStockRequest("ENTRADA", 10)).exchange();

        // Pera en Sede Norte (stock 25)
        ProductoResponse p2 = webTestClient.post().uri(basePath())
            .bodyValue(new CrearProductoRequest("Pera"))
            .exchange().expectBody(ProductoResponse.class).returnResult().getResponseBody();
        webTestClient.patch().uri(basePath() + "/" + p2.id() + "/stock")
            .bodyValue(new ModificarStockRequest("ENTRADA", 25)).exchange();

        // Uva en Sede Sur (stock 5)
        ProductoResponse p3 = webTestClient.post()
            .uri("/api/v1/franquicias/" + franquicia.id() + "/sucursales/" + sedeSur.id() + "/productos")
            .bodyValue(new CrearProductoRequest("Uva"))
            .exchange().expectBody(ProductoResponse.class).returnResult().getResponseBody();
        webTestClient.patch().uri("/api/v1/franquicias/" + franquicia.id() + "/sucursales/" + sedeSur.id() + "/productos/" + p3.id() + "/stock")
            .bodyValue(new ModificarStockRequest("ENTRADA", 5)).exchange();

        webTestClient.get().uri("/api/v1/franquicias/" + franquicia.id() + "/productos/max-stock")
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(com.franquicias.infrastructure.web.dto.ProductoMaxStockResponse.class)
            .value(list -> {
                assertThat(list).hasSize(2);
                assertThat(list).extracting(r -> r.producto().nombre())
                    .containsExactlyInAnyOrder("Pera", "Uva");
            });
    }

    @Test
    void retorna404AlPedirMaxStockDeFranquiciaInexistente() {
        webTestClient.get().uri("/api/v1/franquicias/" + java.util.UUID.randomUUID() + "/productos/max-stock")
            .exchange()
            .expectStatus().isNotFound();
    }
}
